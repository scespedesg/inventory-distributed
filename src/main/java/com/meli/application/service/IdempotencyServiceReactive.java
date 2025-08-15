package com.meli.application.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meli.infrastructure.repository.IdempotencyKeyEntity;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.hibernate.reactive.mutiny.Mutiny;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

@ApplicationScoped
public class IdempotencyServiceReactive {

  private static final ObjectMapper M = new ObjectMapper()
      .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
      .setSerializationInclusion(JsonInclude.Include.NON_NULL);

  /**
   * Ejecuta la acción dentro de la misma sesión/tx reactiva del recurso (@WithTransaction).
   * - Si la key no existe: inserta PENDING, ejecuta acción, guarda respuesta, marca COMPLETED.
   * - Si existe COMPLETED: devuelve misma respuesta.
   * - Si existe PENDING: devuelve 202.
   * - Si el hash difiere: 409.
   */
  public Uni<Response> execute(String key, String requestHash, Supplier<Uni<Response>> action, Mutiny.Session session) {
    return session.find(IdempotencyKeyEntity.class, key)
      .onItem().transformToUni(existing -> {
        if (existing != null) {
          if (!existing.requestHash.equals(requestHash))
            return Uni.createFrom().failure(new WebApplicationException("Idempotency-Key reused with different payload", 409));
          if ("COMPLETED".equals(existing.status))
            return Uni.createFrom().item(rebuild(existing));
          return Uni.createFrom().item(Response.status(202).entity("Processing").build());
        }

        // Insert PENDING (la PK maneja carreras)
        var rec = new IdempotencyKeyEntity();
        rec.id = key;
        rec.requestHash = requestHash;
        rec.status = "PENDING";
        rec.createdAt = Instant.now();
        rec.expiresAt = rec.createdAt.plus(Duration.ofHours(48));

        return session.persist(rec)
          .chain(() -> action.get()
            .onItem().invoke(resp -> {
              rec.httpStatus = resp.getStatus();
              rec.responseJson = toJson(resp.getEntity());
              rec.status = "COMPLETED"; // dirty tracking
            })
          )
          .onFailure().recoverWithUni(t ->
            session.find(IdempotencyKeyEntity.class, key)
              .onItem().ifNotNull().transform(respRec -> {
                if (!respRec.requestHash.equals(requestHash))
                  throw new WebApplicationException("Idempotency-Key reused with different payload", 409);
                if ("COMPLETED".equals(respRec.status)) return rebuild(respRec);
                return Response.status(202).entity("Processing").build();
              })
          );
      });
  }

  public static String hash(String method, String path, Object body) {
    try {
      var canonical = (body == null) ? "" : M.writeValueAsString(body);
      var bytes = (method + "|" + path + "|" + canonical).getBytes(StandardCharsets.UTF_8);
      var md = MessageDigest.getInstance("SHA-256");
      var out = md.digest(bytes);
      var sb = new StringBuilder();
      for (byte b : out) sb.append(String.format("%02x", b));
      return sb.toString();
    } catch (Exception e) { throw new RuntimeException(e); }
  }

  private static String toJson(Object o) {
    try { return o==null ? null : M.writeValueAsString(o); }
    catch (Exception e) { throw new RuntimeException(e); }
  }

  private static Response rebuild(IdempotencyKeyEntity r) {
    try {
      var node = (r.responseJson == null) ? null : M.readTree(r.responseJson);
      return Response.status(r.httpStatus).entity(node).build();
    } catch (Exception e) { throw new RuntimeException(e); }
  }
}
