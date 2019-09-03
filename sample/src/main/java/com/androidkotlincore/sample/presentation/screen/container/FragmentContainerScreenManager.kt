package com.androidkotlincore.sample.presentation.screen.container

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment

/**
 * Created by Peter on 05.02.2018.
 */
interface FragmentContainerScreenManager {
    fun <F : Fragment> start(context: Context, clazz: Class<F>, arguments: Bundle = Bundle())
}