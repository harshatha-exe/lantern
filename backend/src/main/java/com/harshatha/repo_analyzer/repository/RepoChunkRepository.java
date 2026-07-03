package com.harshatha.repo_analyzer.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
public interface RepoChunkRepository extends JpaRepository<RepoChunk, UUID> {

    // ---> ADD THIS NEW CUSTOM INSERT METHOD <---
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO repo_chunks (id, repository_id, file_path, content, embedding) " +
                   "VALUES (gen_random_uuid(), :repoId, :filePath, :content, cast(:embedding as vector))", 
           nativeQuery = true)
    void insertChunkWithVector(
        @Param("repoId") UUID repoId,
        @Param("filePath") String filePath,
        @Param("content") String content,
        @Param("embedding") String embeddingJsonArray
    );

    @Query(value = "SELECT file_path as filePath, content, similarity FROM match_repo_chunks(:repoId, cast(:embedding as vector), :threshold, :limit)", nativeQuery = true)
    List<ChunkSearchResult> searchSimilarChunks(
        @Param("repoId") UUID repoId, 
        @Param("embedding") String embeddingJsonArray, 
        @Param("threshold") double threshold, 
        @Param("limit") int limit
    );

    @Modifying
    @Transactional
    void deleteByRepositoryId(UUID repositoryId);

    interface ChunkSearchResult {
        String getFilePath();
        String getContent();
        Double getSimilarity();
    }
}