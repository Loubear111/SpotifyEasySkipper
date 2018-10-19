/**
 ******************************************************************************
 * @file    MainActivity.java
 * @author  Louis Barrett
 * @brief   Main control class for app
 *
 ******************************************************************************
 * @attention
 *
 * Copyright (c) 2018 Louis Barrett
 * Email: louisbarrett98@gmail.com
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 ******************************************************************************
 */

package com.bear.lou.spotifyeasyskipper;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.speech.tts.TextToSpeech;
import java.util.Locale;
import java.util.Calendar;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "693fb71f54b14d0487b85f6573a70c2e";
    private static final String REDIRECT_URI = "http://localhost:8888/callback/";

    private SpotifyAppRemote mSpotifyAppRemote;

    TextToSpeech t1;

    private boolean isPaused = true;
    private boolean newSong = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button buttonBckwd = (Button) findViewById(R.id.buttonBckwd);
        buttonBckwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                mSpotifyAppRemote.getPlayerApi().skipPrevious();
                newSong = true;
                //updateTrack();
            }
        });

        final Button buttonFwd = (Button) findViewById(R.id.buttonFwd);
        buttonFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                mSpotifyAppRemote.getPlayerApi().skipNext();
                newSong = true;
                //updateTrack();
            }
        });

        buttonFwd.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v){
                // Subscribe to PlayerState
                mSpotifyAppRemote.getPlayerApi()
                        .subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {

                    public void onEvent(PlayerState playerState) {
                        isPaused = playerState.isPaused;
                    }
                });

                if(isPaused) {
                    mSpotifyAppRemote.getPlayerApi().resume();
                    newSong = true;
                }
                else {
                    mSpotifyAppRemote.getPlayerApi().pause();
                }
                return true;
            }
        });

        buttonBckwd.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v){
                sayTrack(getTime());
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        //Forcibly set these so we don't mess up the state we're in
        isPaused = true;
        newSong = false;

        //Initialize TTS!
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);

                    //Comment out to change voice, default is fine for now
                    //Voice v = new Voice("en-us-x-sfg#female_4-local",
                    //        Locale.forLanguageTag("en-us-x-sfg#female_4-local"), 1, 1, false, null);
                    //t1.setVoice(v);

                    t1.setSpeechRate(1.5f); //Set speech rate a little higher
                }
            }
        });

        // Set the connection parameters
        ConnectionParams connectionParams =
                new ConnectionParams.Builder(CLIENT_ID)
                        .setRedirectUri(REDIRECT_URI)
                        .showAuthView(true)
                        .build();

        SpotifyAppRemote.CONNECTOR.connect(this, connectionParams,
                new Connector.ConnectionListener() {

                    @Override
                    public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                        mSpotifyAppRemote = spotifyAppRemote;
                        Log.d("MainActivity", "Connected! Yay!");

                        // Now you can start interacting with App Remote
                        connected();
                    }

                    @Override
                    public void onFailure(Throwable throwable) {
                        Log.e("MainActivity", throwable.getMessage(), throwable);

                        // Something went wrong when attempting to connect! Handle errors here
                    }
                });
    }

    private void connected() {
        mSpotifyAppRemote.getPlayerApi().pause();

        try {
            Thread.sleep(500);
        }catch(java.lang.InterruptedException e) {
            Log.e("MainActivity", "Error delaying.");
        }

        // Subscribe to PlayerState
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState()
                .setEventCallback(new Subscription.EventCallback<PlayerState>() {

                    public void onEvent(PlayerState playerState) {
                        final Track track = playerState.track;
                        if (track != null) {
                            Log.d("MainActivity", track.name + " by " + track.artist.name);

                            if(newSong) {
                                sayTrack(track.name + " by " + track.artist.name);
                                if(!track.name.equals("")){
                                    newSong = false;
                                }
                            }
                        }
                    }
                });

        //mSpotifyAppRemote.getPlayerApi().resume();

    }

    @Override
    protected void onStop() {

        SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);

        // Don't forget to shutdown tts!
        if (t1 != null) {
            t1.stop();
            t1.shutdown();
        }
        super.onStop();
    }

    private void sayTrack(String track) {
        t1.speak(track, TextToSpeech.QUEUE_FLUSH, null);
    }

    private String getTime(){
        Calendar c = Calendar.getInstance();

        String time = "";
        int hour = c.get(Calendar.HOUR);
        if(hour < 1) {
            time += "12";
        }
        else {
            time += hour;
        }

        int minute =  c.get(Calendar.MINUTE);
        if(minute < 10) {
            time += "0" + minute;
        }
        else {
            time += minute;
        }

        if(c.getTime().getHours() > 11){
            time += " PM";
        }
        else{
            time += " AM";
        }

        return time;
    }
}
