package br.com.diegoalexandro.jukebox;

public enum Events {

    LIST("jukebox.list"),
    SCHEDULE("jukebox.schedule"),
    PLAY("jukebox.play"),
    PAUSE("jukebox.pause");


    private String value;

    Events(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
