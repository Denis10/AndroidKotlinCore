package com.androidkotlincore.mvp_rx

import com.androidkotlincore.mvp.addons.CompositeEventListener
import io.reactivex.Observable

/**
 * Created by Peter on 31.01.18.
 */
fun <TEvent> CompositeEventListener<TEvent>.rx(): Observable<TEvent> {
    return Observable.create<TEvent> { emitter ->
        subscribe { event -> emitter.onNext(event) }
    }
}