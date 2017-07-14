/**
 * Copyright (C) 2017-2017 - All rights reserved.
 * This file is part of the telepathdb project which is released under the GPLv3 license.
 * See file LICENSE.txt or go to http://www.gnu.org/licenses/gpl.txt for full license details.
 * You may use, distribute and modify this code under the terms of the GPLv3 license.
 */

package com.telepathdb.costmodel;

import com.telepathdb.datamodels.ParseTree;

final public class CostModel {

  static public int cost(ParseTree tree) {

    return tree.level();
  }
}