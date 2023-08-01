package com.ynixt.sharedfinances.jobs

import com.ynixt.sharedfinances.service.GroupInviteService
import org.quartz.DisallowConcurrentExecution
import org.quartz.Job
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired

@DisallowConcurrentExecution
class ExpireGroupInviteJob : Job {
    @Autowired
    private lateinit var groupInviteService: GroupInviteService

    override fun execute(context: JobExecutionContext?) {
        groupInviteService.deleteAllExpiredInvites()
    }
}
