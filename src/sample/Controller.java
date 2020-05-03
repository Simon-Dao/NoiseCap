package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Slider;
import javafx.scene.media.AudioClip;
import javafx.scene.media.MediaPlayer;

import javax.sound.sampled.*;
import java.io.ByteArrayOutputStream;

public class Controller {

    boolean running = false;

    VolumeListener volumeListener;

    @FXML
    public void startButton() {
        if (!running) {
            start();
        }
    }

    @FXML
    public void stopButton() {
        if (running) {
            stop();
        }
    }

    private void start() {
        running = true;

        volumeListener = new VolumeListener(this);
        //check the audio stream from mic and play bruh sound effect 2 when over a certain decibel

    }

    private void stop() {
        running = false;

        //stop the thread
        volumeListener.targetThread.targetLine.stop();
        volumeListener.targetThread.targetLine.close();
        volumeListener.targetThread.thread.interrupt();
    }

    @FXML
    public Slider volume;
    public Slider sensitivity;

    @FXML
    public double getVolume() {
        return volume.getValue();
    }

    @FXML
    public double getSensitivity() {
        return sensitivity.getValue();
    }
}

class VolumeListener {

    TargetThread targetThread;
    Controller parent;

    public VolumeListener(Controller controller) {

        this.parent = controller;

        init();
    }

    public void init() {

        try {

            AudioFormat format =
                    new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,
                            44_100,
                            16,
                            2,
                            4,
                            44_100, false);

            //the audio stream from the mic
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            final TargetDataLine targetLine = (TargetDataLine) AudioSystem.getLine(info);
            targetLine.open();

            //writes the audio data into a dynamic byte array
            final ByteArrayOutputStream out = new ByteArrayOutputStream();
            targetThread = new TargetThread(parent, targetLine, out);

            targetThread.thread.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class TargetThread implements Runnable {

    TargetDataLine targetLine;
    ByteArrayOutputStream out;
    Thread thread;
    Controller controller;

    AudioClip bruhSoundEffect2;

    public TargetThread(Controller controller, TargetDataLine targetLine, ByteArrayOutputStream out) {
        this.targetLine = targetLine;
        this.out = out;
        this.controller = controller;

        thread = new Thread(this);
    }

    @Override
    public void run() {
        int level = 0;
        byte tempBuffer[] = new byte[targetLine.getBufferSize() / 10];

        targetLine.start();

        while (!thread.isInterrupted()) {
            //gets the audio from the byte array
            int bytesRead = targetLine.read(tempBuffer, 0, tempBuffer.length);

            //saves the audio in memory
            out.write(tempBuffer, 0, bytesRead);

            if (bytesRead >= 0) {
                level = calculateRMSLevel(tempBuffer);

                System.out.println(level);

                if (level >= controller.getSensitivity()) {

                    //tell you to shutup when being too loud
                    playBruhSoundEffect();

                }
            }
        }
    }

    public int calculateRMSLevel(byte[] audioData) {
        long lSum = 0;
        for (int i = 0; i < audioData.length; i++)
            lSum = lSum + audioData[i];

        double dAvg = lSum / audioData.length;
        double sumMeanSquare = 0d;

        for (int j = 0; j < audioData.length; j++)
            sumMeanSquare += Math.pow(audioData[j] - dAvg, 2d);

        double averageMeanSquare = sumMeanSquare / audioData.length;

        return (int) (Math.pow(averageMeanSquare, 0.5d) + 0.5);
    }

    public void playBruhSoundEffect() {
        bruhSoundEffect2 = new AudioClip(this.getClass().getResource("bruh.mp4").toString());
        bruhSoundEffect2.setVolume((int)controller.getVolume() * 2);
        bruhSoundEffect2.play();
    }
}