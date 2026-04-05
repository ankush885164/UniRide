package com.example.unifront

import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.realtime.Realtime

object SupabaseProvider {
    val client = createSupabaseClient(
        supabaseUrl = "https://taqrnuwokzofqqwjynni.supabase.co",
        supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InRhcXJudXdva3pvZnFxd2p5bm5pIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzQ4NDM4NzIsImV4cCI6MjA5MDQxOTg3Mn0.S25M-LECVWBmGBcTInaOGtHiVL1mWCBttbPi-eUBhs0"
    ) {
        install(Postgrest)
        install(Auth)
        install(Realtime)
    }
}
