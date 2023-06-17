package com.coroutine.demo.practice

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/* Job Lifecycle

- Coroutine Builder 를 통해 Job 이 생성되면 기본적으로 Active 상태로 생성됨. Active 는 작업(Task)을 실행 중인 상태이다.
- New 상태로 만들기 위해서는 CoroutineStart.LAZY 사용
- Coroutine 또한 예기치 못한 상황이 발생하면 바로 Cancelled 로 가는게 아니라 Cancelling 과정에서 "데이터의 정합성을 맞춰주는 과정" 과 같이 예기치 못한 상황으로 종료되었을때 자원을 반납한다던지의 과정을 이 State 에서 수행한다.

                                      wait children
+-----+ start  +--------+ complete   +-------------+  finish  +-----------+
| New | -----> | Active | ---------> | Completing  | -------> | Completed |
+-----+        +--------+            +-------------+          +-----------+
                 |  cancel / fail       |
                 |     +----------------+
                 |     |
                 V     V
             +------------+                           finish  +-----------+
             | Cancelling | --------------------------------> | Cancelled |
             +------------+                                   +-----------+
 */

suspend fun main(): Unit = coroutineScope {

}

// Coroutine State Practice
suspend fun state(): Unit = coroutineScope {
    val parentJob = coroutineContext[Job]
    println("[ParentJob] $parentJob")

    val job = launch(parentJob!!) {
        val childJob = coroutineContext[Job]
        println(parentJob == childJob) // false
        println(parentJob.children.first() == childJob) // true
    }

    val newStateJob = launch(start = CoroutineStart.LAZY) {
        println("[LAZY-JOB] START!")
        delay(1000)
        println("[LAZY-JOB] END!")
    }

    job.join()
    newStateJob.join()
}

// Structured Concurrency Practice
// Task 가 완료(completed)가 되기 위해서는 sub-task 들이 먼저 완료가 되어야 한다.
// 하나의 sub-task 가 실패하면 Task 는 실패한다.
suspend fun structuredConcurrency(): Unit = coroutineScope {
    val parentJob = launch {
        println("Parent Job is Start!!")

        val childJob = launch {
            delay(200)
            println("Child Job 1 is Start!!")
        }

        val childJob2 = launch {
            delay(300)
            println("Child Job 2 is Start!!")
        }

        println("Child Job1 is Finished ? ${childJob.isCompleted}") // false
        println("Child Job2 is Finished ? ${childJob2.isCompleted}") // false

        // SubParentJob 자체의 연산은 성공했을 수 있지만, 내부에 있는 childJob1, childJob2 의 성공 여부는 아직 확인할 수 없다.
        // 따라서, SubParentJob 을 completed 상태로 곧바로 바꿔버린다면, 내부에 있는 Job 들의 실행완료를 보장해줄 수 없게 된다.
        // 일단은 Completed 가 아닌 Completing 상태에 머물게 된다.
        println("Parent Job is Done!!")
    }

    delay(100)

    parentJob.printChildJobState()
    // isCompleted docs 를 보면 다음과 같다.
    // Job becomes complete only after all its children complete.
    println("[IMPORTANT-LOG-1] Parent Job is Finished ? ${parentJob.isCompleted}") // false

    delay(300)
    parentJob.printChildJobState()
    println("[IMPORTANT-LOG-2] Parent Job is Finished ? ${parentJob.isCompleted}") // true
}

fun Job.printChildJobState() {
    if (this.children.firstOrNull() == null) println("All ChildrenJob is finished")
    var count = 1
    for (childJob in this.children) {
        println("Child Job$count is Finished ? ${childJob.isCompleted}")
        count++
    }
}


