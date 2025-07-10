package com.notifyme.controller;

import com.notifyme.dto.ApiResponse;
import com.notifyme.entity.TQuery;
import com.notifyme.entity.TUser;
import com.notifyme.service.QueryService;
import com.notifyme.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/queries")
@CrossOrigin(origins = {"https://notificamy.com", "https://www.notificamy.com", "http://localhost:3000", "http://localhost:5173"}, 
             allowCredentials = "true", maxAge = 3600)
public class QueryController {

    private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

    @Autowired
    private QueryService queryService;

    @Autowired
    private UserService userService;

    /**
     * Ottiene tutte le query dell'utente autenticato
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<TQuery>>> getUserQueries(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            List<TQuery> queries = queryService.findByUser(user);
            
            logger.info("Retrieved {} queries for user: {}", queries.size(), userEmail);
            return ResponseEntity.ok(ApiResponse.success("Queries retrieved successfully", queries));
            
        } catch (Exception e) {
            logger.error("Error retrieving user queries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving queries: " + e.getMessage()));
        }
    }

    /**
     * Ottiene solo le query attive dell'utente
     */
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<TQuery>>> getActiveQueries(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            List<TQuery> activeQueries = queryService.findActiveQueriesByUser(user);
            
            logger.info("Retrieved {} active queries for user: {}", activeQueries.size(), userEmail);
            return ResponseEntity.ok(ApiResponse.success("Active queries retrieved successfully", activeQueries));
            
        } catch (Exception e) {
            logger.error("Error retrieving active queries: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving active queries: " + e.getMessage()));
        }
    }

    /**
     * Ottiene query per tipo
     */
    @GetMapping("/type/{type}")
    public ResponseEntity<ApiResponse<List<TQuery>>> getQueriesByType(@PathVariable String type, 
                                                                     HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            List<TQuery> queries;
            switch (type.toLowerCase()) {
                case "cron":
                    queries = queryService.findCronQueriesByUser(user);
                    break;
                case "specific":
                    queries = queryService.findSpecificQueriesByUser(user);
                    break;
                case "check":
                    queries = queryService.findCheckQueriesByUser(user);
                    break;
                default:
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Invalid query type. Use: cron, specific, or check"));
            }
            
            logger.info("Retrieved {} {} queries for user: {}", queries.size(), type, userEmail);
            return ResponseEntity.ok(ApiResponse.success(type + " queries retrieved successfully", queries));
            
        } catch (Exception e) {
            logger.error("Error retrieving {} queries: {}", type, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving " + type + " queries: " + e.getMessage()));
        }
    }

    /**
     * Ottiene statistiche delle query dell'utente
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<QueryService.QueryStatistics>> getQueryStatistics(HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            QueryService.QueryStatistics stats = queryService.getQueryStatistics(user);
            
            logger.info("Retrieved query statistics for user: {} - total: {}, cron: {}, specific: {}, check: {}", 
                       userEmail, stats.getTotalQueries(), stats.getCronQueries(), 
                       stats.getSpecificQueries(), stats.getCheckQueries());
            
            return ResponseEntity.ok(ApiResponse.success("Query statistics retrieved successfully", stats));
            
        } catch (Exception e) {
            logger.error("Error retrieving query statistics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving query statistics: " + e.getMessage()));
        }
    }

    /**
     * Chiude una query specifica
     */
    @PutMapping("/{queryId}/close")
    public ResponseEntity<ApiResponse<String>> closeQuery(@PathVariable Long queryId, 
                                                         HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            boolean success = queryService.closeQuery(queryId, user);
            
            if (success) {
                logger.info("Query {} closed successfully by user: {}", queryId, userEmail);
                return ResponseEntity.ok(ApiResponse.success("Query closed successfully", "OK"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Failed to close query. Query not found or access denied."));
            }
            
        } catch (Exception e) {
            logger.error("Error closing query {}: {}", queryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error closing query: " + e.getMessage()));
        }
    }

    /**
     * Ottiene una query specifica per ID
     */
    @GetMapping("/{queryId}")
    public ResponseEntity<ApiResponse<TQuery>> getQueryById(@PathVariable Long queryId, 
                                                           HttpServletRequest request) {
        try {
            String userEmail = (String) request.getAttribute("userEmail");
            TUser user = userService.findOrCreateUser(userEmail);
            
            if (user == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            // Trova la query e verifica che appartenga all'utente
            List<TQuery> userQueries = queryService.findByUser(user);
            TQuery query = userQueries.stream()
                    .filter(q -> q.getId().equals(queryId))
                    .findFirst()
                    .orElse(null);
            
            if (query == null) {
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Retrieved query {} for user: {}", queryId, userEmail);
            return ResponseEntity.ok(ApiResponse.success("Query retrieved successfully", query));
            
        } catch (Exception e) {
            logger.error("Error retrieving query {}: {}", queryId, e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Error retrieving query: " + e.getMessage()));
        }
    }
}