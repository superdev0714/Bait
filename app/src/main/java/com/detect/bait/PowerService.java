package com.detect.bait;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;


/**
 * Created by hello on 12/8/17.
 */

public class PowerService extends JobService {

    static final int JOB_ID = 1000;
    Intent intent = null;

    @Override
    public boolean onStartJob(JobParameters parameters) {

        startService(new Intent(getBaseContext(), PowerService.class));

        return false;
    }

    @Override
    public boolean onStopJob(JobParameters parameters) {
        return true;
    }

}
