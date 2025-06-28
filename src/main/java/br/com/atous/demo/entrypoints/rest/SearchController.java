package br.com.atous.demo.entrypoints.rest;

import br.com.atous.demo.application.port.in.QuantumSearchUseCase;
import br.com.atous.demo.domain.model.GroverResult;
import br.com.atous.demo.entrypoints.rest.dto.SearchRequest;
import br.com.atous.demo.entrypoints.rest.dto.SearchResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final QuantumSearchUseCase searchUseCase;

    public SearchController(QuantumSearchUseCase searchUseCase) {
        this.searchUseCase = searchUseCase;
    }

    @PostMapping("/execute")
    public ResponseEntity<SearchResponse> executeGroverSearch(@RequestBody SearchRequest request) {
        if (request.searchSpaceSize() <= 0 || request.targetIndex() >= request.searchSpaceSize()) {
            return ResponseEntity.badRequest().body(new SearchResponse("Invalid input parameters.", null));
        }
        
        GroverResult result = searchUseCase.executeSearch(request.searchSpaceSize(), request.targetIndex());
        
        String message = result.success()? "Search successful!" : "Search failed to find the target.";
        return ResponseEntity.ok(new SearchResponse(message, result));
    }
} 