package com.example.ceris.view.fragment

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.example.ceris.R
import com.example.ceris.local.SessionManager
import com.example.ceris.view.LoginActivity
import com.example.ceris.view.adapter.LocalAdapter
import com.example.ceris.view.renderLocais
import com.example.ceris.viewmodel.LocaisStatus
import com.example.ceris.viewmodel.LocaisViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class LocaisFragment : Fragment(R.layout.fragment_locais) {
    private val viewModel: LocaisViewModel by viewModels()
    private var addressNotice: AlertDialog? = null

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.init(SessionManager(requireContext()))
        val adapter = LocalAdapter()
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
            } else {
                viewModel.refresh()
            }
        }
        // O cadastro nao pode ser usado para editar uma conta existente.
        view.findViewById<View>(R.id.btn_alterar).setOnClickListener {
            addressNotice?.dismiss()
            addressNotice =
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.locais_alterar)
                    .setMessage(R.string.locais_edicao_indisponivel)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
        }
        parentFragmentManager.setFragmentResultListener(ADDRESS_SAVED, viewLifecycleOwner) { _, _ -> viewModel.refresh() }
        if (!isHidden) viewModel.onVisible()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { renderLocais(view, adapter, it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!isHidden) viewModel.onVisible()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden && view != null) viewModel.onVisible()
    }

    override fun onDestroyView() {
        addressNotice?.dismiss()
        addressNotice = null
        view?.findViewById<RecyclerView>(R.id.lista_locais)?.adapter = null
        super.onDestroyView()
    }

    companion object {
        /** A edicao de perfil publica este resultado somente depois de salvar. */
        const val ADDRESS_SAVED = "profile_address_saved"
    }
}
