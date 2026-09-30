package com.pampoukidis.streamcoretv.web.storage

/** Application startup classification for the SDK's sanitized storage failure. */
internal class WebSdkStorageException : IllegalStateException("SDK browser storage I/O is unavailable.")
