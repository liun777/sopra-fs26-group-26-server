package ch.uzh.ifi.hase.soprafs26.rest.dto;


// representation for a response to the client, with potentially filtered values
public class CardViewDTO {

    private int position;
    private boolean faceDown;
    private Integer value;
    private String code;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public boolean isFaceDown() {
        return faceDown;
    }

    public void setFaceDown(boolean faceDown) {
        this.faceDown = faceDown;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
