package com.ericsson.oss.mediation.fm.o1.handlers.test

import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommand
import com.ericsson.oss.itpf.sdk.core.retry.RetriableCommandException
import com.ericsson.oss.itpf.sdk.core.retry.RetryManager
import com.ericsson.oss.itpf.sdk.core.retry.RetryPolicy

class RetryManagerTest implements RetryManager {
    @Override
    <T> T executeCommand(final RetryPolicy retryPolicy, final RetriableCommand<T> retriableCommand) throws IllegalArgumentException, RetriableCommandException {
        return retriableCommand.execute()
    }
}

