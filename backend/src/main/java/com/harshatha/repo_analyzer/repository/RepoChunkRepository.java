package com.harshatha.repo_analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RepoChunkRepository extends JpaRepository<RepoChunk, UUID> {

    @Query(value = "SELECT file_path as filePath, content, similarity FROM match_repo_chunks(:repoId, cast(:embedding as vector), :threshold, :limit)", nativeQuery = true)
    List<ChunkSearchResult> searchSimilarChunks(
        @Param("repoId") UUID repoId, 
        @Param("embedding") String embeddingJsonArray, 
        @Param("threshold") double threshold, 
        @Param("limit") int limit
    );

    interface ChunkSearchResult {
        String getFilePath();
        String getContent();
        Double getSimilarity();
    }
}