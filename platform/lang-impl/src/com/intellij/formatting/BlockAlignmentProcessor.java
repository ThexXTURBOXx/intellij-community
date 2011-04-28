/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.formatting;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

/**
 * Stands for block alignment strategy (e.g. we may want to use different strategies for the different
 * {@link Alignment.Anchor alignment acnhors}).
 * 
 * @author Denis Zhdanov
 * @since 4/28/11 3:55 PM
 */
public interface BlockAlignmentProcessor {
  
  enum Result {

    /**
     * This value should be used to indicate that alignment of the target block can't be performed because it doesn't have
     * a counterparty (e.g. we want to align two blocks and this value is returned after processing the first of them).
     */
    TARGET_BLOCK_PROCESSED_NOT_ALIGNED,
    
    /** Alignment is performed for the target block. */
    TARGET_BLOCK_ALIGNED,

    /** Already processed block was realigned because of {@link AlignmentImpl#isAllowBackwardShift() backward alignment}. */
    BACKWARD_BLOCK_ALIGNED,

    /**
     * It was necessary to align already processed block because of {@link AlignmentImpl#isAllowBackwardShift() backward alignment}
     * but that can't be done (e.g. that backward block {@link AbstractBlockWrapper#getWhiteSpace() white space} is read-only).
     */
    UNABLE_TO_ALIGN_BACKWARD_BLOCK
  }

  /**
   * Asks current processor to perform alignment processing for the parameters encapsulated at the given context.
   * 
   * @param context     target parameters holder
   * @return            processing result
   */
  Result applyAlignment(@NotNull Context context);
  
  class Context {
    
    @NotNull public final AlignmentImpl alignment;
    @NotNull public final LeafBlockWrapper targetBlock;
    @NotNull public final Map<AbstractBlockWrapper, Set<AbstractBlockWrapper>> alignmentMappings;
    @NotNull public final Map<LeafBlockWrapper, Set<LeafBlockWrapper>> backwardShiftedAlignedBlocks;
    
    @Nullable public LeafBlockWrapper backwardAlignmentTarget;

    public Context(@NotNull AlignmentImpl alignment,
                   @NotNull LeafBlockWrapper targetBlock,
                   @NotNull Map<AbstractBlockWrapper, Set<AbstractBlockWrapper>> alignmentMappings,
                   @NotNull Map<LeafBlockWrapper, Set<LeafBlockWrapper>> backwardShiftedAlignedBlocks)
    {
      this.alignment = alignment;
      this.targetBlock = targetBlock;
      this.alignmentMappings = alignmentMappings;
      this.backwardShiftedAlignedBlocks = backwardShiftedAlignedBlocks;
    }
  }
}
