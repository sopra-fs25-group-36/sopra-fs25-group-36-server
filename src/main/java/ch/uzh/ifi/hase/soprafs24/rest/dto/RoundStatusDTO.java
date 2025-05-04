package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class RoundStatusDTO {
    private boolean allSubmitted;
    private boolean roundEnded;

    public RoundStatusDTO() { }

    public RoundStatusDTO(boolean allSubmitted, boolean roundEnded) {
        this.allSubmitted = allSubmitted;
        this.roundEnded    = roundEnded;
    }

    public boolean isAllSubmitted() {
        return allSubmitted;
    }
    public void setAllSubmitted(boolean allSubmitted) {
        this.allSubmitted = allSubmitted;
    }
    public boolean isRoundEnded() {
        return roundEnded;
    }
    public void setRoundEnded(boolean roundEnded) {
        this.roundEnded = roundEnded;
    }
}
