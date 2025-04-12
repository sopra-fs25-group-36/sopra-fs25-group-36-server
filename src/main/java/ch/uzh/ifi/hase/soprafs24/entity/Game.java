package ch.uzh.ifi.hase.soprafs24.entity;

import javax.persistence.GeneratedValue;
import java.io.Serializable;
import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.game.Transaction;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    @Id
    // @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lobbyId;

    @Column(nullable = false)
    private int currentRound;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    // Add fields as needed (e.g., stock data, player portfolios)

    @PrePersist
    protected void onCreate() {
        this.startedAt = LocalDateTime.now();
        this.currentRound = 1;
        this.active = true;
    }

    // Getters and Setters...
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(Long lobbyId) {
        this.lobbyId = lobbyId;
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public void setCurrentRound(int currentRound) {
        this.currentRound = currentRound;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

//    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
//    private List<Transaction> transactions = new ArrayList<>();

//    public List<Transaction> getTransactions() {
//        return transactions;
//    }
//
//    public void addTransaction(Transaction txn) {
//        transactions.add(txn);
//    }

}
