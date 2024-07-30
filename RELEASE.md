Release Instructions
--------------------

Pre-Commit
----------
1. `pushd ngrok-java-native && fix-n-fmt && popd`
1. `export NGROK_AUTHTOKEN="<your_authtoken>"; make all`

Prepare Release
---------------
NOTE: this step needs to make commits to main, which will be blocked by branch controls. You will
need to get in contact with an admin to temporarily remove the branch control while completing this step.

1. In the `ngrok-java` repo, click on Actions in the top.
1. Select the "Release Prepare" option
1. On the right side, select "Run workflow"
1. For `releaseVersion`, put in the desired release version
   1. do NOT include a `v` in front of the version number
1. For `developmentVersion`, put in the following minor version, appended with `-SNAPSHOT`
   1. For instance, if releasing `1.6.3`, the `developmentVersion` should be `1.7.0-SNAPSHOT`
   1. do NOT include a `v` in front of the version number
1. Press "Run workflow"
1. Verify that the workflow runs successfully
1. Once done, check the commit history, there should be two new commits

Release
-------

1. In the `ngrok-java` repo, click on Actions in the top.
1. Select the "Release Perform" option
1. On the right side, select "Run workflow"
1. For `releaseVersion`, put in the release version you specified in the "Prepare Release" workflow
   1. NOTE: do NOT include a `v` in front of the version number
1. Press "Run workflow"
1. Verify that the workflow runs successfully
