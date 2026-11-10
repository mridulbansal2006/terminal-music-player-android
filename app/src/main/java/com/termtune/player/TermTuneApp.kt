package com.termtune.player

import android.app.Application

/**
 * Application entry point.
 *
 * TermTune deliberately keeps construction simple: the view model owns the
 * repository, settings store and player controller, so there is no DI graph to
 * build here. The class exists so the manifest has a stable application name
 * and a place for future process-wide setup.
 */
class TermTuneApp : Application()
