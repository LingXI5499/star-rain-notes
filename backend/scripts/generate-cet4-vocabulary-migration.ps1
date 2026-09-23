param(
    [Parameter(Mandatory = $true)]
    [string] $SourceZipPath,

    [string] $OutputPath = (Join-Path $PSScriptRoot '../src/main/resources/db/migration/V35__replace_vocabulary_with_cet4.sql')
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression.FileSystem

function Quote-Sql([AllowNull()][string] $Value) {
    if ($null -eq $Value) { return 'NULL' }
    return "'" + $Value.Replace('\', '\\').Replace("'", "''") + "'"
}

if (-not (Test-Path -LiteralPath $SourceZipPath -PathType Leaf)) {
    throw "CET4 source ZIP not found: $SourceZipPath"
}

$archive = [IO.Compression.ZipFile]::OpenRead((Resolve-Path -LiteralPath $SourceZipPath).Path)
try {
    $jsonEntry = $archive.Entries | Where-Object { $_.FullName -like '*/多主题分类词库.json' -or $_.FullName -like '*/CET4_ONLY_4190_多主题语义分类.json' } | Select-Object -First 1
    if (-not $jsonEntry) { throw 'The ZIP does not contain the canonical CET4 JSON file.' }

    $reader = [IO.StreamReader]::new($jsonEntry.Open(), [Text.Encoding]::UTF8)
    try { $data = $reader.ReadToEnd() | ConvertFrom-Json -AsHashtable }
    finally { $reader.Dispose() }

    $categories = @($data.metadata.categories)
    $words = @($data.words)
    if ($words.Count -ne 4190 -or $data.metadata.unique_headwords -ne 4190) {
        throw "Expected 4,190 unique headwords, found $($words.Count)."
    }
    if ($categories.Count -ne 60 -or $data.metadata.taxonomy_categories -ne 60) {
        throw "Expected 60 categories, found $($categories.Count)."
    }

    $themeSeedPath = Join-Path $PSScriptRoot '../src/main/resources/db/migration/V4__seed_vocabulary.sql'
    $themeSeed = [IO.File]::ReadAllText((Resolve-Path -LiteralPath $themeSeedPath).Path, [Text.Encoding]::UTF8)
    $seedRows = @([regex]::Matches($themeSeed, "INSERT INTO vocabulary_theme \(id, layer, layer_order, name, sort_order\) VALUES \((\d+), '([^']*)', (\d+), '([^']*)', (\d+)\);") | ForEach-Object {
        [pscustomobject]@{ Id = [int]$_.Groups[1].Value; Name = $_.Groups[4].Value }
    })
    if ($seedRows.Count -ne $categories.Count) { throw 'The CET4 categories do not match the 60 existing vocabulary theme slots.' }
    for ($index = 0; $index -lt $categories.Count; $index++) {
        if ($seedRows[$index].Name -cne $categories[$index].category) {
            throw "Category mismatch at theme id $($seedRows[$index].Id): expected '$($categories[$index].category)', found '$($seedRows[$index].Name)'."
        }
    }

    $categoryToThemeId = @{}
    $categoryToFile = @{}
    $categoryOrder = @{}
    $themeUpdates = [Collections.Generic.List[string]]::new()
    for ($index = 0; $index -lt $categories.Count; $index++) {
        $category = $categories[$index]
        $id = $seedRows[$index].Id
        $categoryToThemeId["$($category.layer)|$($category.category)"] = $id
        $categoryToFile["$($category.layer)|$($category.category)"] = $category.file
        $categoryOrder["$($category.layer)|$($category.category)"] = $index + 1
        if ($category.layer -notmatch '^(\d+)\s+(.+)$') { throw "Invalid layer name: $($category.layer)" }
        $layerOrder = [int]$Matches[1]
        $layerName = $Matches[2]
        $themeUpdates.Add("INSERT INTO vocabulary_theme (id, layer, layer_order, name, sort_order) VALUES ($id,$(Quote-Sql $layerName),$layerOrder,$(Quote-Sql $category.category),$($index + 1)) ON DUPLICATE KEY UPDATE layer=$(Quote-Sql $layerName), layer_order=$layerOrder, name=$(Quote-Sql $category.category), sort_order=$($index + 1);")
    }

    $usageRows = 0
    foreach ($word in $words) { $usageRows += @($word.category_usages).Count }
    if ($usageRows -ne 7053 -or $usageRows -ne $data.metadata.classification_occurrences) {
        throw "Expected 7,053 category placements, found $usageRows."
    }

    $markdownOrder = @{}
    foreach ($fileName in @($categories.file | Sort-Object -Unique)) {
        $entryName = ($jsonEntry.FullName -replace '[^/]+$', '') + $fileName
        $markdownEntry = $archive.GetEntry($entryName)
        if (-not $markdownEntry) { throw "Missing category Markdown file: $fileName" }
        $markdownReader = [IO.StreamReader]::new($markdownEntry.Open(), [Text.Encoding]::UTF8)
        try { $lines = $markdownReader.ReadToEnd() -split "`r?`n" }
        finally { $markdownReader.Dispose() }

        $currentCategory = $null
        foreach ($line in $lines) {
            if ($line -match '^##\s*\d+[.．、]?\s*(.+?)\s*[（(]\d+\s*条[）)]\s*$') {
                $currentCategory = $Matches[1].Trim()
                continue
            }
            if ($currentCategory -and $line -match '^\|\s*(\d+)\s*\|\s*\*\*(.+?)\*\*\s*\|') {
                $rowNumber = [int]$Matches[1]
                $headword = $Matches[2].Trim().ToLowerInvariant()
                $key = "$fileName|$currentCategory|$headword"
                $markdownOrder[$key] = $rowNumber
            }
        }
    }

    $sortCounters = @{}
    $unmappedPlacements = [Collections.Generic.List[string]]::new()
    $rows = [Collections.Generic.List[object]]::new()
    foreach ($word in $words) {
        foreach ($usage in @($word.category_usages)) {
            $categoryKey = "$($usage.layer)|$($usage.category)"
            if (-not $categoryToThemeId.ContainsKey($categoryKey)) { throw "Unknown source category: $categoryKey" }
            $themeId = $categoryToThemeId[$categoryKey]
            if (-not $sortCounters.ContainsKey($themeId)) { $sortCounters[$themeId] = 0 }
            $sortCounters[$themeId]++
            $sortOrder = $sortCounters[$themeId]
            $markdownKey = "$($categoryToFile[$categoryKey])|$($usage.category)|$($word.headword.ToLowerInvariant())"
            if ($markdownOrder.ContainsKey($markdownKey)) {
                $sortOrder = $markdownOrder[$markdownKey]
            } else {
                $unmappedPlacements.Add("$($word.headword) @ $($usage.category)")
            }

            if ($word.headword.Length -gt 200 -or $word.pos.Length -gt 50 -or $word.us_ipa.Length -gt 100 -or $word.uk_ipa.Length -gt 100 -or $word.cn_meaning.Length -gt 1000 -or $usage.scene_meaning.Length -gt 1000) {
                throw "Source field exceeds vocabulary_word limits for '$($word.headword)' in '$($usage.category)'."
            }

            $rows.Add([pscustomobject]@{
                ThemeId = $themeId
                Pos = [string]$word.pos
                Word = [string]$word.headword
                UsIpa = [string]$word.us_ipa
                UkIpa = [string]$word.uk_ipa
                Meaning = [string]$word.cn_meaning
                SceneMeaning = [string]$usage.scene_meaning
                SortOrder = $sortOrder
            })
        }
    }
    if ($rows.Count -ne 7053) { throw "Generated $($rows.Count) word placements instead of 7,053." }
    if ($unmappedPlacements.Count -gt 0) {
        throw "Could not match $($unmappedPlacements.Count) placements to the source Markdown ordering. First: $($unmappedPlacements[0])"
    }

    $sql = [Collections.Generic.List[string]]::new()
    $sql.Add('-- Generated from the canonical CET4 JSON and Markdown files; regenerate with backend/scripts/generate-cet4-vocabulary-migration.ps1.')
    $sql.Add('-- Replaces the old seeded vocabulary while preserving the existing vocabulary tables and study endpoints.')
    $sql.Add('SET NAMES utf8mb4;')
    $sql.Add('DELETE FROM vocabulary_word;')
    $sql.AddRange($themeUpdates)
    $batchSize = 200
    for ($offset = 0; $offset -lt $rows.Count; $offset += $batchSize) {
        $limit = [Math]::Min($offset + $batchSize, $rows.Count)
        $sql.Add('INSERT INTO vocabulary_word (theme_id, part_of_speech, word, phonetic_us, phonetic_uk, translation, scene_meaning, inflections, examples, sort_order) VALUES')
        $values = [Collections.Generic.List[string]]::new()
        for ($index = $offset; $index -lt $limit; $index++) {
            $row = $rows[$index]
            $values.Add("($($row.ThemeId),$(Quote-Sql $row.Pos),$(Quote-Sql $row.Word),$(Quote-Sql $row.UsIpa),$(Quote-Sql $row.UkIpa),$(Quote-Sql $row.Meaning),$(Quote-Sql $row.SceneMeaning),NULL,'[]',$($row.SortOrder))")
        }
        $sql.Add(($values -join ",`n"))
        $sql.Add(';')
    }
    $sql.Add('-- Expected result: 60 themes, 7,053 categorized word rows, 4,190 unique headwords.')

    $outputTarget = if ([IO.Path]::IsPathRooted($OutputPath)) { $OutputPath } else { Join-Path $PSScriptRoot $OutputPath }
    $resolvedOutput = [IO.Path]::GetFullPath($outputTarget)
    [IO.Directory]::CreateDirectory([IO.Path]::GetDirectoryName($resolvedOutput)) | Out-Null
    [IO.File]::WriteAllLines($resolvedOutput, $sql, [Text.UTF8Encoding]::new($false))
    "Wrote $($rows.Count) placements across $($categories.Count) themes to $resolvedOutput"
}
finally {
    $archive.Dispose()
}
