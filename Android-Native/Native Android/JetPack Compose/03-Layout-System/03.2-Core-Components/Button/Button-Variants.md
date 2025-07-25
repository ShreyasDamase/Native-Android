# Button-Variants Overview

## Theory & Interview Knowledge

### Material Design 3 Button Hierarchy

Button variants in Jetpack Compose follow Material Design 3 principles, creating a visual hierarchy that guides user attention and interaction patterns.

**Visual Hierarchy (High to Low Emphasis):**

1. **FloatingActionButton** - Highest emphasis, primary screen action
2. **Button (Filled)** - High emphasis, primary actions
3. **ElevatedButton** - Medium-high emphasis, important secondary actions
4. **FilledTonalButton** - Medium emphasis, secondary actions
5. **OutlinedButton** - Medium-low emphasis, alternative actions
6. **TextButton** - Low emphasis, tertiary/dismissive actions
7. **IconButton** - Context-dependent, space-efficient actions

### Key Interview Points

**Q: Why do we need different button variants?**

- **Visual Affordances**: Different styles communicate different levels of importance
- **Cognitive Load**: Helps users understand action hierarchy without reading every label
- **Accessibility**: Provides multiple visual cues for different user needs
- **Design Consistency**: Follows established Material Design patterns

**Q: How does button hierarchy improve UX?**

- **Scanability**: Users can quickly identify primary vs secondary actions
- **Decision Making**: Reduces cognitive overhead by visually prioritizing options
- **Error Prevention**: Primary actions are visually distinct from destructive ones
- **Progressive Disclosure**: Less important actions don't compete for attention

### Technical Implementation Concepts

```kotlin
// All buttons share common interface but different visual treatments
@Composable
fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    content: @Composable RowScope.() -> Unit
)
```

### Performance Considerations

- **Recomposition**: Use `remember` for click handlers
- **Drawing Overhead**: TextButton < OutlinedButton < FilledButton < ElevatedButton
- **Touch Targets**: All buttons maintain minimum 48dp touch target
- **Animation**: State changes (pressed, disabled) are automatically animated

### Best Practices Summary

| Variant              | Primary Use         | Max Per Screen | Typical Placement          |
| -------------------- | ------------------- | -------------- | -------------------------- |
| FloatingActionButton | Primary action      | 1              | Bottom-right, contextual   |
| Button (Filled)      | Main CTA            | 1-2            | Prominent position         |
| ElevatedButton       | Important secondary | 2-3            | Secondary prominence       |
| FilledTonalButton    | Secondary actions   | Multiple       | Supporting areas           |
| OutlinedButton       | Alternative actions | Multiple       | Choice scenarios           |
| TextButton           | Tertiary/Cancel     | Multiple       | Dialog actions, navigation |
| IconButton           | Tool actions        | Multiple       | Toolbars, compact spaces   |

### Common Interview Questions

1. **"When would you use FilledTonalButton over OutlinedButton?"**
    
    - FilledTonalButton: When you need more visual weight than outlined but less than filled
    - OutlinedButton: When emphasizing the boundary/choice aspect
2. **"How do you handle button states in Compose?"**
    
    - Automatic state handling via `enabled` parameter
    - Custom states via `interactionSource` and `collectIsPressedAsState()`
    - Loading states via conditional content rendering
3. **"What's the difference between elevation and tonal variants?"**
    
    - Elevation: Physical depth metaphor, better for light themes
    - Tonal: Color-based emphasis, better for dark themes and accessibility
4. **"How do you ensure button accessibility?"**
    
    - Minimum touch targets (48dp)
    - Semantic descriptions via `contentDescription`
    - State descriptions for dynamic buttons
    - Sufficient color contrast ratios