package com.bukload.transit;

import com.bukload.transit.client.dto.TransitRouteResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/transit")
@RequiredArgsConstructor
public class TransitController {

    private final TransitService transitService;

    // api/transit/routes?sx=126.9779692&sy=37.566535&ex=127.027621&ey=37.497942
    @GetMapping("/routes")
    public Mono<TransitRouteResponse> getRoutes(
            @RequestParam double sx,
            @RequestParam double sy,
            @RequestParam double ex,
            @RequestParam double ey,
            @RequestParam(required = false) Integer searchPathType
    ) {
        return transitService.searchBestRoute(sx, sy, ex, ey, searchPathType);
    }
}
