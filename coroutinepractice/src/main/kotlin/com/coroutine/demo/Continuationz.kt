package com.coroutine.demo

import kotlin.coroutines.Continuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlin.coroutines.CoroutineContext

suspend fun main(): Unit = coroutineScope {
    val continuation = SharedDataContinuation(
        completion = Continuation(
            context = Dispatchers.Main,
            resumeWith = {}
        )
    )

    println("${Thread.currentThread()} =====Start=====")
    val firstResult = function(continuation)
    println("${Thread.currentThread()} FiresResult : $firstResult")
    println("=====Second===== ${Thread.currentThread()}")
    val secondResult = function(continuation)
    println("${Thread.currentThread()} SecondResult : $secondResult")
    println("${Thread.currentThread()} =====End=====")
}

fun function(
    // 연속적인 데이터를 계속해서 주입 받아 이용하기 위함으로 CPS Pattern
    // CPS Pattern = 함수형 프로그래밍에서 Continuation 형태로 제어권을 명시적으로 넘기는 것
    continuation: Continuation<Unit>
): Any {
    continuation as SharedDataContinuation

    if (continuation.label == 0) {
        // 연속적인 데이터를 다른 Thread 에서도 이용하기 위해 continuation 에서 꺼내서 사용
        continuation.thisLocalVariable = 10
        continuation.thisLocalVariable2 = "Local Value"

        println("${Thread.currentThread()} Start!!")

        continuation.label = 1

        // COROUTINE_SUSPEND 값은, 현재 Coroutine 이 suspend(일시중단) 되었음을 알려주기 위함
        if (delay(1000, continuation) == "COROUTINE_SUSPEND") {
            return "COROUTINE_SUSPEND"
        }
    } // Thread Free And Local Data Clear

    if (continuation.label == 1) {
        println(continuation.thisLocalVariable)
        println(continuation.thisLocalVariable2)
        println("${Thread.currentThread()} End")
        return Unit
    }

    error("정상적인 종료가 아님")
}

/**
 * Continuation 의 경우 실제 Function 에서 Thread Stack 영역에 물고 있어야 하는 정보를 저장하고 있음
 */
class SharedDataContinuation(
    private val completion: Continuation<Any>,
): Continuation<Unit> {
    override val context: CoroutineContext
        get() = completion.context

    override fun resumeWith(result: Result<Unit>) {
        this.result = result
        val res = try {
            // 다시 함수를 호출시킬때는 현재 continuation 을 넣어준다.
            val r = function(continuation = this)

            // COROUTINE_SUSPEND 가 들어오게 되면 그냥 함수를 나가버린다. = Thread Free
            if (r == "COROUTINE_SUSPEND") return

            Result.success(r as Unit)
        } catch (e: Throwable) {
            Result.failure(e)
        }
        completion.resumeWith(res)
    }

    var result: Result<Unit>? = null

    // 해당 Data 들은 Thread 가 변경되어도 알고 있어야 하는 Data 이므로 Continuation 에 저장해야 하므로 Continuation 에 저장
    var label = 0
    var thisLocalVariable = 10
    var thisLocalVariable2 = "Local Value"
}

/**
 * 예제를 위한 임시 구현 함수
 */
fun delay(delayTime: Int, continuation: Continuation<Unit>): String {
    return "COROUTINE_SUSPEND"
}

/**
 * // 원본
 * suspend fun originalFunction() {
 *     var thisLocalVariable: Int = 10
 *     var thisLocalVariable2: String = "Local Value"
 *
 *     println("Start!!")
 *     kotlinx.coroutines.delay(1000)
 *     println(thisLocalVariable)
 *     println(thisLocalVariable2)
 *     println("End")
 * }
 */