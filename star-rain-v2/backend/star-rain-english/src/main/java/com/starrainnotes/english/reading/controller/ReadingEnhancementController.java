package com.starrainnotes.english.reading.controller;
import com.starrainnotes.common.result.ApiResponse;
import com.starrainnotes.english.reading.dto.ReadingEnhancementDto.*;
import com.starrainnotes.english.reading.service.ReadingEnhancementService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class ReadingEnhancementController {
    private final ReadingEnhancementService service;
    @GetMapping("/api/public/english/content/reading/{slug}/{kind:alignments|annotations|vocabulary}")
    public ApiResponse<List<Item>> publicList(@PathVariable String slug,@PathVariable String kind) { return ApiResponse.ok(service.list(slug,false,kind)); }
    @GetMapping("/api/admin/english/content/reading/{id}/{kind:alignments|annotations|vocabulary}")
    @PreAuthorize("hasAuthority('english:content-read-admin')")
    public ApiResponse<List<Item>> adminList(@PathVariable String id,@PathVariable String kind) { return ApiResponse.ok(service.list(id,true,kind)); }
    @PutMapping("/api/admin/english/content/reading/{id}/{kind:alignments|annotations|vocabulary}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> replace(@PathVariable String id,@PathVariable String kind,@RequestBody Batch batch) { return ApiResponse.ok(service.replace(id,kind,batch)); }
    @PostMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> create(@PathVariable String id,@PathVariable String kind,@RequestBody Item item) { return ApiResponse.ok(service.save(id,kind,null,item)); }
    @PutMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}/{itemId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> update(@PathVariable String id,@PathVariable String kind,@PathVariable long itemId,@RequestBody Item item) { return ApiResponse.ok(service.save(id,kind,itemId,item)); }
    @DeleteMapping("/api/admin/english/content/reading/{id}/{kind:annotations|vocabulary}/{itemId}")
    @PreAuthorize("hasAuthority('english:content-edit')")
    public ApiResponse<Result> delete(@PathVariable String id,@PathVariable String kind,@PathVariable long itemId,@RequestParam long rowVersion) { return ApiResponse.ok(service.delete(id,kind,itemId,rowVersion)); }
}
