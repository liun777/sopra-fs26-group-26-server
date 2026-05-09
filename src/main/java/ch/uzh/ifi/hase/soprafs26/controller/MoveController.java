package ch.uzh.ifi.hase.soprafs26.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ch.uzh.ifi.hase.soprafs26.rest.dto.MoveLogEntryDTO;
import ch.uzh.ifi.hase.soprafs26.service.MoveService;

@RestController
public class MoveController {

    private final MoveService moveService;

    public MoveController(MoveService moveService) {
        this.moveService = moveService;
    }

    // #111: log of the requester's own moves + others' public moves in this session
    @GetMapping("/sessions/{sessionId}/log")
    @ResponseStatus(HttpStatus.OK)
    public List<MoveLogEntryDTO> getSessionLog(@PathVariable String sessionId,
                                               @RequestHeader("Authorization") String token) {
        return moveService.getSessionLog(sessionId, token);
    }
}
