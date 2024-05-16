package ch.epfl.chacun.logic;

public record ServerActionWithData(ServerAction action, String data) {

    public ServerActionWithData(ServerAction action) {
        this(action, null);
    }

    @Override
    public String toString() {
        return STR."\{action}\{data == null ? "" : STR.".\{data}"}";
    }
}
