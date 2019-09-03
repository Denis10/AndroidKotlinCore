package com.androidkotlincore.sample.presentation.screen.splash

import androidx.lifecycle.Lifecycle
import com.androidkotlincore.mvp.impl.launchCoroutineUI
import com.androidkotlincore.sample.presentation.base.BasePresenterImpl
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * Created by Peter on 05.02.2018.
 */
class SplashPresenterImpl : BasePresenterImpl<SplashPresenter, SplashView>(), SplashPresenter {

    override fun onCreated(isFirstPresenterCreation: Boolean) {
        super.onCreated(isFirstPresenterCreation)

        //example of lifecycle usage
        lateinit var startLoginScreenJob: Job

        viewLifecycle.subscribe { event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> startLoginScreenJob = launchCoroutineUI { startLoginScreen() }
                Lifecycle.Event.ON_PAUSE -> startLoginScreenJob.cancel()
                Lifecycle.Event.ON_CREATE -> doSomething()
                Lifecycle.Event.ON_START -> doSomething()
                Lifecycle.Event.ON_STOP -> doSomething()
                Lifecycle.Event.ON_DESTROY -> doSomething()
                else -> {
                    //never happened
                }
            }
        }
    }

    private suspend fun startLoginScreen() {
        //some long operation
        delay(2_000)
        //open login screen and finish this screen
        postToView {
            openLoginScreen()
            finish()
        }
    }

    private fun doSomething() {

    }
}