/*
 * Copyright (C) 2024 - present Instructure, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.instructure.parentapp.features.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.view.GravityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavController.Companion.KEY_DEEP_LINK_INTENT
import androidx.navigation.fragment.NavHostFragment
import com.instructure.canvasapi2.models.User
import com.instructure.loginapi.login.tasks.LogoutTask
import com.instructure.pandautils.interfaces.NavigationCallbacks
import com.instructure.pandautils.utils.ColorKeeper
import com.instructure.pandautils.utils.ViewStyler
import com.instructure.pandautils.utils.animateCircularBackgroundColorChange
import com.instructure.pandautils.utils.applyTheme
import com.instructure.pandautils.utils.showThemed
import com.instructure.parentapp.R
import com.instructure.parentapp.databinding.FragmentDashboardBinding
import com.instructure.parentapp.databinding.NavigationDrawerHeaderLayoutBinding
import com.instructure.parentapp.util.ParentLogoutTask
import com.instructure.parentapp.util.ParentPrefs
import com.instructure.parentapp.util.navigation.Navigation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class DashboardFragment : Fragment(), NavigationCallbacks {

    private lateinit var binding: FragmentDashboardBinding

    private val viewModel: DashboardViewModel by viewModels()

    @Inject
    lateinit var navigation: Navigation

    private lateinit var navController: NavController
    private lateinit var headerLayoutBinding: NavigationDrawerHeaderLayoutBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDashboardBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupNavigation()

        lifecycleScope.launch {
            viewModel.data.collectLatest {
                setupNavigationDrawerHeader(it.userViewData)
                setupAppColors(it.selectedStudent)
            }
        }

        handleDeeplink()
    }

    private fun setupNavigation() {
        val navHostFragment = childFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        navController.graph = navigation.createDashboardNavGraph(navController)

        setupToolbar()
        setupNavigationDrawer()
        setupBottomNavigationView()
    }

    private fun handleDeeplink() {
        try {
            val uri = BundleCompat.getParcelable(
                arguments ?: return,
                KEY_DEEP_LINK_INTENT,
                Intent::class.java
            )?.data

            uri?.let {
                navController.navigate(it)
            }
        } catch (e: Exception) {
            Log.e(this.javaClass.simpleName, e.message.orEmpty())
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        toolbar.setNavigationIcon(R.drawable.ic_hamburger)
        toolbar.navigationContentDescription = getString(R.string.navigation_drawer_open)
        toolbar.setNavigationOnClickListener {
            openNavigationDrawer()
        }
    }

    private fun setupNavigationDrawer() {
        val navView = binding.navView

        headerLayoutBinding = NavigationDrawerHeaderLayoutBinding.bind(navView.getHeaderView(0))

        navView.setNavigationItemSelectedListener {
            closeNavigationDrawer()
            when (it.itemId) {
                R.id.inbox -> menuItemSelected { navigation.navigate(activity, navigation.inbox) }
                R.id.manage_students -> menuItemSelected { navigation.navigate(activity, navigation.manageStudents) }
                R.id.settings -> menuItemSelected { navigation.navigate(activity, navigation.settings) }
                R.id.help -> menuItemSelected { navigation.navigate(activity, navigation.help) }
                R.id.log_out -> menuItemSelected { onLogout() }
                R.id.switch_users -> menuItemSelected { onSwitchUsers() }
                else -> false
            }
        }
    }

    private fun menuItemSelected(action: () -> Unit): Boolean {
        action()
        return true
    }

    private fun setupBottomNavigationView() {
        val bottomNavigationView = binding.bottomNav

        bottomNavigationView.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.courses -> navigateWithPopBackStack(navigation.courses)
                R.id.calendar -> navigateWithPopBackStack(navigation.calendar)
                R.id.alerts -> navigateWithPopBackStack(navigation.alerts)
                else -> false
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val menuId = when (destination.route) {
                navigation.courses -> R.id.courses
                navigation.calendar -> R.id.calendar
                navigation.alerts -> R.id.alerts
                else -> return@addOnDestinationChangedListener
            }
            bottomNavigationView.menu.findItem(menuId).isChecked = true
        }
    }

    private fun navigateWithPopBackStack(route: String): Boolean {
        navController.popBackStack()
        navController.navigate(route)
        return true
    }

    private fun setupAppColors(student: User?) {
        val color = ColorKeeper.getOrGenerateUserColor(student).backgroundColor()
        if (binding.toolbar.background == null) {
            binding.toolbar.setBackgroundColor(color)
        } else {
            binding.toolbar.animateCircularBackgroundColorChange(color, binding.toolbarImage)
        }
        binding.bottomNav.applyTheme(color, requireActivity().getColor(R.color.textDarkest))
        ViewStyler.setStatusBarDark(requireActivity(), color)
    }

    private fun openNavigationDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    private fun closeNavigationDrawer() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupNavigationDrawerHeader(userViewData: UserViewData?) {
        headerLayoutBinding.userViewData = userViewData
    }

    private fun onLogout() {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_warning)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                ParentLogoutTask(LogoutTask.Type.LOGOUT).execute()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .showThemed(ColorKeeper.getOrGenerateUserColor(ParentPrefs.currentStudent).textAndIconColor())
    }

    private fun onSwitchUsers() {
        ParentLogoutTask(LogoutTask.Type.SWITCH_USERS).execute()
    }

    override fun onHandleBackPressed(): Boolean {
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            closeNavigationDrawer()
            return true
        }
        return false
    }
}