# STRICT ACCEPTANCE CRITERIA

Every feature must behave like a professional mobile video editor.

Do not implement simplified interactions.

Before considering any feature complete, verify that all of the following are true.

## Timeline

- Timeline never jumps unexpectedly.
- Timeline scrolling is smooth.
- No lag.
- No flickering.
- Horizontal scrolling is pixel accurate.
- Dragging one clip never causes the whole timeline to move unexpectedly.
- Only the intended clip moves.
- While trimming, only the selected edge changes.
- Neighbor clips stay correct.
- Timeline auto-scrolls only when the user's finger reaches the edge.
- Timeline zoom never breaks thumbnail alignment.
- Timeline never loses sync with preview.

## Clip Selection

- Tapping a clip always selects exactly one clip.
- Selection highlight updates instantly.
- No accidental deselection.

## Trimming

- Left trim changes only the clip start.
- Right trim changes only the clip end.
- Trimming updates preview in real time.
- Trimming never changes another clip.
- Handles are easy to grab.
- Trimming is frame accurate.

## Preview

- Preview updates instantly.
- No black frames.
- No flicker.
- Preview always matches timeline.

## Playback

- Playhead stays fixed.
- Timeline moves underneath.
- Playback remains synchronized.

## Ranking

- Active clip automatically highlights its title.
- Highlight changes exactly when playback enters the next clip.
- No delay.
- No incorrect highlighting.

## Text

- Text can be dragged smoothly.
- No jumping.
- Size changes smoothly.
- Color updates instantly.

## Volume

- Each clip has independent volume.
- Volume changes affect only that clip.

## Export

- Exported video must be identical to preview.
- Same timing.
- Same trims.
- Same text positions.
- Same ranking highlights.
- Same colors.
- Same quality.

## Performance

- UI target 60 FPS.
- No dropped frames.
- No memory leaks.
- No crashes during editing.
- No crashes during export.
- Responsive on mid-range Android devices.

## Code Quality

- No placeholder code.
- No fake implementation.
- No TODO comments.
- No unfinished methods.
- No temporary hacks.

If any requirement is not fully implemented, stop and fix it before moving to the next feature.