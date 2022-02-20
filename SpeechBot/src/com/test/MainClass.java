package com.test;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.speech.AudioManager;
import javax.speech.EngineManager;
import javax.speech.synthesis.SpeakableEvent;
import javax.speech.synthesis.SpeakableListener;
import javax.speech.synthesis.Synthesizer;
import javax.speech.synthesis.SynthesizerEvent;
import javax.speech.synthesis.SynthesizerListener;
import javax.speech.synthesis.SynthesizerMode;

import org.jvoicexml.jsapi2.synthesis.freetts.FreeTTSEngineListFactory;

public class MainClass implements SpeakableListener, SynthesizerListener{

    private MainClass() {
    }

    /**
     * Starts this demo.
     * @param args command line arguments.
     */
    public static void main(final String[] args) {
        // Enable logging at all levels.
        Handler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        Logger.getLogger("").addHandler(handler);
        Logger.getLogger("").setLevel(Level.ALL);

        try {
            EngineManager
                    .registerEngineListFactory(FreeTTSEngineListFactory.class
                            .getName());
            // Create a synthesizer for the default Locale
            Synthesizer synthesizer = (Synthesizer) EngineManager
                    .createEngine(SynthesizerMode.DEFAULT);
            MainClass demo = new MainClass();
            final AudioManager manager = synthesizer.getAudioManager();
//            manager.setMediaLocator("file:/home/dirk/test.wav");
            synthesizer.addSynthesizerListener(demo);
            // Get it ready to speak
            synthesizer.allocate();
            synthesizer.resume();
            synthesizer.waitEngineState(Synthesizer.RESUMED);

            // Speak the "hello world" string
            System.out.println("Speaking 'Hello, world!'...");
            synthesizer.speak("Hello, world!", demo);
            synthesizer.speakMarkup("<?xml version=\"1.0\"?>"
                    + "<speak>Goodbye!</speak>", demo);
            synthesizer.waitEngineState(Synthesizer.QUEUE_EMPTY);
            System.out.println("done.");
            // Clean up - includes waiting for the queue to empty
            synthesizer.deallocate();
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void speakableUpdate(SpeakableEvent e) {
        System.out.println(e);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void synthesizerUpdate(SynthesizerEvent e) {
        System.out.println(e);
    }
	
}
