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
import java.util.concurrent.TimeUnit;

import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.PlayerApi;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import com.spotify.protocol.client.CallResult;
import com.spotify.protocol.client.ErrorCallback;
import com.spotify.protocol.client.Result;
import com.spotify.protocol.client.Subscription;
import com.spotify.protocol.types.PlayerState;
import com.spotify.protocol.types.Track;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "693fb71f54b14d0487b85f6573a70c2e";
    private static final String REDIRECT_URI = "http://localhost:8888/callback/";

    private SpotifyAppRemote mSpotifyAppRemote;
    private PlayerApi playerApi;

    TextToSpeech t1;

    private boolean isPaused = false;
    private String currentTrack = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        final Button buttonBckwd = (Button) findViewById(R.id.buttonBckwd);
        buttonBckwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                mSpotifyAppRemote.getPlayerApi().skipPrevious();
                updateTrack();
            }
        });

        final Button buttonFwd = (Button) findViewById(R.id.buttonFwd);
        buttonFwd.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Code here executes on main thread after user presses button
                mSpotifyAppRemote.getPlayerApi().skipNext();
                updateTrack();
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

                sayTrack();

                if(isPaused) {
                    //mSpotifyAppRemote.getPlayerApi().resume();
                }
                else {
                    //mSpotifyAppRemote.getPlayerApi().pause();
                }
                return true;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
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

        mSpotifyAppRemote.getPlayerApi().resume();

        updateTrack();
    }

    @Override
    protected void onStop() {
        super.onStop();

        SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);

        // Don't forget to shutdown tts!
        if (t1 != null) {
            t1.stop();
            t1.shutdown();
        }
    }

    private void updateTrack() {
        // Subscribe to PlayerState
        mSpotifyAppRemote.getPlayerApi()
                .subscribeToPlayerState().setEventCallback(new Subscription.EventCallback<PlayerState>() {
            @Override
            public void onEvent(PlayerState playerState) {
                final Track track = playerState.track;
                if (track != null) {
                    currentTrack = track.name;
                    //Log.e("MainActivity", "Current track: " + currentTrack);
                }
            }
        });
    }

    private void sayTrack() {
        t1.speak(currentTrack, TextToSpeech.QUEUE_FLUSH, null);
    }

}
