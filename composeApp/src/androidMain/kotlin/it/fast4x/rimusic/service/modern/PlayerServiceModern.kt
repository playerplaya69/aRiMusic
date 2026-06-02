override fun onPlayerError(error: PlaybackException) {
        super.onPlayerError(error)

        Timber.e("PlayerServiceModern onPlayerError error code ${error.errorCode} message ${error.message} cause ${error.cause?.cause}")
        println("PlayerServiceModern onPlayerError error code ${error.errorCode} message ${error.message} cause ${error.cause?.cause}")

        val playbackConnectionExeptionList = listOf(
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, //primary error code to manage
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT
        )

        // Fixed: Added null safety check for error.cause before type casting
        val isConnectionError = try {
            (error.cause?.cause is PlaybackException)
                    && (error.cause?.cause as? PlaybackException)?.errorCode in playbackConnectionExeptionList
        } catch (e: Exception) {
            Timber.e("Error checking connection error type: ${e.stackTraceToString()}")
            false
        }

        if (!isNetworkAvailable.value || isConnectionError) {
            waitingForNetwork.value = true
            SmartMessage(resources.getString(R.string.error_no_internet), context = this )
            return
        }

        val playbackHttpExeptionList = listOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_READ_POSITION_OUT_OF_RANGE,
            416 // 416 Range Not Satisfiable
        )

        if (error.errorCode in playbackHttpExeptionList) {
            Timber.e("PlayerServiceModern onPlayerError recovered occurred errorCodeName ${error.errorCodeName} cause ${error.cause?.cause}")
            println("PlayerServiceModern onPlayerError recovered occurred errorCodeName ${error.errorCodeName} cause ${error.cause?.cause}")
            try {
                player.pause()
                player.prepare()
                player.play()
            } catch (e: Exception) {
                Timber.e("Failed to recover from HTTP error: ${e.stackTraceToString()}")
            }
            return
        }

        if (!preferences.getBoolean(skipMediaOnErrorKey, false) || !player.hasNextMediaItem())
            return

        // Fixed: Added null checking for currentMediaItem before accessing it
        val prev = player.currentMediaItem
        if (prev == null) {
            Timber.w("Current media item is null, cannot skip to next")
            return
        }

        try {
            player.playNext()
            showSmartMessage(
                message = getString(
                    R.string.skip_media_on_error_message,
                    prev.mediaMetadata.title ?: "Unknown"
                )
            )
        } catch (e: Exception) {
            Timber.e("Failed to skip media on error: ${e.stackTraceToString()}")
        }

    }
