// SPDX-License-Identifier: MIT
// This file is part of CassandraGargoyle Community Project
// Licensed under the MIT License - see LICENSE file for details

package engine

import (
	"path"
	"strings"
)

// matchPattern reports whether a slash-separated relative path matches a glob pattern
//
// The pattern is split into segments at "/". A "**" segment matches zero or more
// directories at any position, other segments use path.Match syntax (*, ?, [...]),
// where "*" never crosses a "/"
func matchPattern(pattern, relPath string) bool {
	return matchSegments(splitSegments(pattern), splitSegments(relPath))
}

// matchDirPattern reports whether an exclude pattern removes a whole directory
//
// Only patterns with a trailing "/**" exclude directories, for example "**/target/**"
// excludes "target" and "a/b/target"
func matchDirPattern(pattern, relDir string) bool {
	prefix, ok := strings.CutSuffix(normalizePattern(pattern), "/**")
	if !ok || prefix == "" {
		return false
	}
	return matchPattern(prefix, relDir)
}

// normalizePattern removes a leading "./" from a pattern
func normalizePattern(pattern string) string {
	for strings.HasPrefix(pattern, "./") {
		pattern = pattern[2:]
	}
	return pattern
}

// splitSegments splits a pattern or path into non-empty segments
func splitSegments(value string) []string {
	parts := strings.Split(normalizePattern(value), "/")
	segments := parts[:0]
	for _, part := range parts {
		if part != "" && part != "." {
			segments = append(segments, part)
		}
	}
	return segments
}

// matchSegments matches pattern segments against path segments
func matchSegments(pattern, segments []string) bool {
	for len(pattern) > 0 {
		if pattern[0] == "**" {
			// Collapse repeated "**" segments
			for len(pattern) > 0 && pattern[0] == "**" {
				pattern = pattern[1:]
			}
			if len(pattern) == 0 {
				return true
			}
			for i := 0; i <= len(segments); i++ {
				if matchSegments(pattern, segments[i:]) {
					return true
				}
			}
			return false
		}

		if len(segments) == 0 {
			return false
		}
		matched, err := path.Match(pattern[0], segments[0])
		if err != nil || !matched {
			return false
		}
		pattern = pattern[1:]
		segments = segments[1:]
	}
	return len(segments) == 0
}
