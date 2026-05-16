package ch.uzh.ifi.hase.soprafs26.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class Session extends SessionHistoryBase {
}
