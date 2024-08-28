import hudson.FilePath;

if (build.result.equals(hudson.model.Result.SUCCESS)) {
    if (build.getPreviousBuild().result.isWorseThan(hudson.model.Result.SUCCESS)) {
        status = "WM_BLAME_STATUS=backtonormal";
    }
    else {
        status = "WM_BLAME_STATUS=success";
    }
}
else if (build.result.equals(hudson.model.Result.UNSTABLE)) {
    status = "WM_BLAME_STATUS=testfailure";
}
else {
    status = "WM_BLAME_STATUS=error";
}
FilePath fp = new FilePath(build.getWorkspace(), "env.properties");
content = fp.readToString();
content += status;
fp.write(content, null);
