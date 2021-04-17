package com.artf.chatapp.ui

import android.content.Context
import android.view.View
import androidx.arch.core.executor.testing.CountingTaskExecutorRule
import androidx.fragment.app.testing.FragmentScenario
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.ViewMatchers.hasChildCount
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.artf.chatapp.R
import com.artf.chatapp.data.model.User
import com.artf.chatapp.ui.util.atPosition
import com.artf.chatapp.ui.util.waitForAdapterChange
import com.artf.chatapp.util.any
import com.artf.chatapp.util.mock
import com.artf.chatapp.util.nullable
import com.artf.chatapp.utils.states.NetworkState
import com.artf.chatapp.view.FirebaseViewModel
import com.artf.chatapp.view.searchUser.SearchAdapter
import com.artf.chatapp.view.searchUser.SearchFragment
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
class SearchFragmentTest {

    @get:Rule
    var countingRule = CountingTaskExecutorRule()

    companion object {
        val viewModel: FirebaseViewModel = mock()
    }

    private val userSearchStatus = MutableLiveData<NetworkState>()
    private val userList = MutableLiveData<List<User>>()
    private lateinit var scenario: FragmentScenario<SearchFragmentTest>
    private lateinit var appContext: Context

    @Before
    fun init() {
        Mockito.reset(viewModel)
        appContext = InstrumentationRegistry.getInstrumentation().context
        `when`(viewModel.userSearchStatus).thenReturn(userSearchStatus)
        `when`(viewModel.userList).thenReturn(userList)
        scenario = launchFragmentInContainer<SearchFragmentTest>()
        scenario.onFragment {
            it.binding.root.visibility = View.VISIBLE
            it.binding.searchView.visibility = View.VISIBLE
        }
    }

    @Test
    fun userSearchStatusRUNNING() {
        userSearchStatus.postValue(NetworkState.LOADING)
        onView(withId(R.id.progressBar)).check(matches(isDisplayed()))
    }

    @Test
    fun userSearchStatusSUCCESS() {
        userSearchStatus.postValue(NetworkState.LOADED)
        val user = User(userId = "1", username = "Test")
        val userList = mutableListOf(user, user, user)
        setRecyclerViewData(userList)
        onView(withId(R.id.recyclerView)).check(matches(hasChildCount(userList.size)))
        val recyclerView = onView((withId(R.id.recyclerView)))
        recyclerView.check(matches(atPosition(0, R.id.photoImageView, isDisplayed())))
    }

    @Test
    fun userSearchStatusFAILED() {
        userSearchStatus.postValue(NetworkState.FAILED)
        onView(withId(R.id.info)).check(matches(isDisplayed()))
    }

    @Test
    fun searchAdapterItemClick() {
        val user = User(userId = "1", username = "Test")
        val userList = mutableListOf(user, user, user)
        setRecyclerViewData(userList)
        onView(withId(R.id.recyclerView)).perform(
            actionOnItemAtPosition<SearchAdapter.MsgViewHolder>(0, ViewActions.click())
        )
        verify(viewModel).setReceiver(nullable())
        verify(viewModel).setFragmentState(any(), any())
    }

    private fun setRecyclerViewData(userList: MutableList<User>) {
        lateinit var rv: RecyclerView
        scenario.onFragment {
            this.userList.value = userList
            rv = it.binding.recyclerView
        }
        waitForAdapterChange(rv, countingRule)
    }

    class SearchFragmentTest : SearchFragment() {

        override fun injectMembers() {
            this.viewModelFactory = ViewModelFactory(viewModel)
        }

        @Suppress("UNCHECKED_CAST")
        class ViewModelFactory<T>(private val mock: T) : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>) = mock as T
        }
    }
}