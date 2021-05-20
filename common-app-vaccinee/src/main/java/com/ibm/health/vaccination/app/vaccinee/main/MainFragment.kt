package com.ibm.health.vaccination.app.vaccinee.main

import android.os.Bundle
import android.view.View
import androidx.core.view.isVisible
import androidx.viewpager2.widget.ViewPager2
import com.ensody.reactivestate.android.autoRun
import com.ensody.reactivestate.android.onDestroyView
import com.ensody.reactivestate.get
import com.ensody.reactivestate.validUntil
import com.google.android.material.tabs.TabLayoutMediator
import com.ibm.health.common.android.utils.buildState
import com.ibm.health.common.android.utils.viewBinding
import com.ibm.health.common.navigation.android.FragmentNav
import com.ibm.health.common.navigation.android.findNavigator
import com.ibm.health.common.vaccination.app.BaseFragment
import com.ibm.health.common.vaccination.app.dialog.DialogModel
import com.ibm.health.common.vaccination.app.dialog.showDialog
import com.ibm.health.vaccination.app.vaccinee.R
import com.ibm.health.vaccination.app.vaccinee.add.AddVaccinationCertificateFragmentNav
import com.ibm.health.vaccination.app.vaccinee.databinding.VaccineeMainBinding
import com.ibm.health.vaccination.app.vaccinee.dependencies.vaccineeDeps
import com.ibm.health.vaccination.app.vaccinee.detail.DetailCallback
import com.ibm.health.vaccination.app.vaccinee.information.VaccinationInformationFragmentNav
import com.ibm.health.vaccination.app.vaccinee.storage.GroupedCertificatesList
import kotlinx.parcelize.Parcelize

@Parcelize
internal class MainFragmentNav : FragmentNav(MainFragment::class)

/**
 * The main fragment hosts a [ViewPager2] to display all Vaccination Certificates and serves as entry point for further actions
 * (e.g. add new Vaccination certificate, show settings screen, show selected Vaccination certificate)
 */
internal class MainFragment : BaseFragment(), DetailCallback {

    private val viewModel by buildState { MainViewModel(scope) }
    private val binding by viewBinding(VaccineeMainBinding::inflate)
    private var fragmentStateAdapter: CertificateFragmentStateAdapter by validUntil(::onDestroyView)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        autoRun {
            updateCertificates(get(vaccineeDeps.certRepository.certs), viewModel.selectedCertId)
        }
    }

    private fun setupViews() {
        binding.mainAddButton.setOnClickListener { showAddVaccinationCertificatePopup() }
        binding.mainSettingsImagebutton.setOnClickListener { findNavigator().push(VaccinationInformationFragmentNav()) }
        fragmentStateAdapter = CertificateFragmentStateAdapter(this)
        fragmentStateAdapter.attachTo(binding.mainViewPager)
        TabLayoutMediator(binding.mainTabLayout, binding.mainViewPager) { _, _ ->
            // no special tab config necessary
        }.attach()
        setupPageChangeCallback()
    }

    private fun setupPageChangeCallback() {
        binding.mainViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                viewModel.onPageSelected(position)
            }
        })
    }

    private fun updateCertificates(certificateList: GroupedCertificatesList, selectedCertId: String?) {
        if (certificateList.certificates.isEmpty()) {
            binding.mainEmptyCardview.isVisible = true
            binding.mainViewPager.isVisible = false
        } else {
            fragmentStateAdapter.createFragments(certificateList)
            binding.mainEmptyCardview.isVisible = false
            binding.mainViewPager.isVisible = true
            selectedCertId?.let {
                binding.mainViewPager.setCurrentItem(fragmentStateAdapter.getItemPosition(it), isResumed)
            }
        }
        binding.mainTabLayout.isVisible = certificateList.certificates.size > 1
    }

    override fun onDeletionCompleted() {
        val dialogModel = DialogModel(
            titleRes = R.string.delete_result_dialog_header,
            messageRes = R.string.delete_result_dialog_message,
            positiveButtonTextRes = R.string.delete_result_dialog_positive_button_text,
        )
        showDialog(dialogModel, childFragmentManager)
    }

    override fun displayCert(certId: String) {
        viewModel.selectedCertId = certId
        binding.mainViewPager.setCurrentItem(fragmentStateAdapter.getItemPosition(certId), isResumed)
    }

    private fun showAddVaccinationCertificatePopup() {
        findNavigator().push(AddVaccinationCertificateFragmentNav())
    }
}
