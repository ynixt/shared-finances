package com.ynixt.sharedfinances.domain.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.produceIn

object FlowMergeSort {
    fun <T> mergeSorted(
        a: Flow<T>,
        b: Flow<T>,
        comparator: Comparator<in T>,
    ): Flow<T> =
        channelFlow {
            val chA = a.produceIn(this)
            val chB = b.produceIn(this)

            var headA = chA.receiveCatching().getOrNull()
            var headB = chB.receiveCatching().getOrNull()

            while (headA != null || headB != null) {
                when {
                    headB == null -> {
                        send(headA!!)
                        headA = chA.receiveCatching().getOrNull()
                    }
                    headA == null -> {
                        send(headB)
                        headB = chB.receiveCatching().getOrNull()
                    }
                    comparator.compare(headA, headB) <= 0 -> {
                        send(headA)
                        headA = chA.receiveCatching().getOrNull()
                    }
                    else -> {
                        send(headB)
                        headB = chB.receiveCatching().getOrNull()
                    }
                }
            }

            chA.cancel()
            chB.cancel()
        }
}
