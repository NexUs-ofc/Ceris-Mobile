package com.example.ceris.view.fragment

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.location.DeviceLocationSource
import com.example.ceris.view.DetalheMercadoActivity
import com.example.ceris.view.LoginActivity
import com.example.ceris.view.adapter.LocalAdapter
import com.example.ceris.view.renderLocais
import com.example.ceris.viewmodel.LocaisStatus
import com.example.ceris.viewmodel.LocaisViewModel
import kotlinx.coroutines.launch

class LocaisFragment : Fragment(R.layout.fragment_locais) {
    private val viewModel: LocaisViewModel by viewModels()
    private var permissionRequested = false
    private val locationPermission =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            viewModel.allowPermissionFallback(!hasLocationPermission())
            if (view != null && !isHidden && isResumed) viewModel.onLocationPermissionResult(hasLocationPermission())
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        permissionRequested = savedInstanceState?.getBoolean(PERMISSION_REQUESTED) ?: permissionRequested
        viewModel.init(SessionManager(requireContext()), DeviceLocationSource(requireContext()))
        viewModel.allowPermissionFallback(permissionRequested && !hasLocationPermission())
        val adapter = LocalAdapter { local -> context?.let { DetalheMercadoActivity.abrir(it, local) } }
        view.findViewById<RecyclerView>(R.id.lista_locais).adapter = adapter
        adapter.stateRestorationPolicy = RecyclerView.Adapter.StateRestorationPolicy.PREVENT_WHEN_EMPTY
        view.findViewById<View>(R.id.btn_tentar_locais).setOnClickListener {
            if (viewModel.state.value.status == LocaisStatus.AUTH_REQUIRED) {
                startActivity(
                    Intent(requireContext(), LoginActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    },
                )
                requireActivity().finish()
            } else if (viewModel.state.value.status == LocaisStatus.LOCATION_PERMISSION) {
                requestLocation()
            } else if (viewModel.state.value.status == LocaisStatus.LOCATION_UNAVAILABLE) {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            } else {
                viewModel.refresh()
            }
        }
        view.findViewById<View>(R.id.btn_alterar).setOnClickListener {
            if (hasLocationPermission()) viewModel.refresh() else requestLocation()
        }
        parentFragmentManager.setFragmentResultListener(ADDRESS_SAVED, viewLifecycleOwner) { _, _ -> viewModel.refresh() }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect {
                    renderLocais(view, adapter, it)
                    if (it.status == LocaisStatus.LOCATION_PERMISSION && !permissionRequested && !isHidden) requestLocation()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) viewModel.onVisible()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null && isResumed) viewModel.onVisible()
        if (hidden) viewModel.onNotVisible()
    }

    override fun onPause() {
        viewModel.onNotVisible()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean(PERMISSION_REQUESTED, permissionRequested)
        super.onSaveInstanceState(outState)
    }

    private fun hasLocationPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestLocation() {
        val blocked = permissionRequested && !shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)
        if (blocked && !hasLocationPermission()) {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${requireContext().packageName}".toUri()))
        } else {
            permissionRequested = true
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    override fun onDestroyView() {
        view?.findViewById<RecyclerView>(R.id.lista_locais)?.adapter = null
        super.onDestroyView()
    }

    companion object {
        private const val PERMISSION_REQUESTED = "location_permission_requested"

        /** A edicao de perfil publica este resultado somente depois de salvar. */
        const val ADDRESS_SAVED = "profile_address_saved"
    }
}
