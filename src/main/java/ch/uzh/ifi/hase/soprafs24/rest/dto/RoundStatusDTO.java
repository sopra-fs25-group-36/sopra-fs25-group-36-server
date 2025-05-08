package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class RoundStatusDTO {
    private boolean allSubmitted;
    private boolean roundEnded;

    private Long nextRoundStartTime;


    public RoundStatusDTO(boolean allSubmitted, boolean roundEnded, long nextRoundStartTime) {
        this.allSubmitted = allSubmitted;
        this.roundEnded    = roundEnded;
        this.nextRoundStartTime = nextRoundStartTime;

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
    public Long getNextRoundStartTime() {
        return nextRoundStartTime;
    }

    public void setNextRoundStartTime(Long nextRoundStartTime) {
        this.nextRoundStartTime = nextRoundStartTime;
    }





}
