package me.timschneeberger.rootlessjamesdsp.session.dump.data

interface ISessionPolicyInfoDump : IDump {
    val capturePermissionLog: HashMap<String , Boolean >
}
