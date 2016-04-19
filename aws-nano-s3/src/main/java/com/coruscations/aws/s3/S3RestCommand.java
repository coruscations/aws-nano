/*
 * Copyright (c) 2016 Michael K. Werle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.coruscations.aws.s3;

import com.coruscations.aws.RestCommand;
import com.coruscations.aws.RestCommandResponse;

import javax.annotation.Nonnull;

interface S3RestCommand<T extends RestCommandResponse> extends RestCommand<T> {

  String S3_SERVICE_NAME = "s3";
  String XMLNS = "http://s3.amazonaws.com/doc/2006-03-01/";

  @Nonnull
  @Override
  default String getServiceName() {
    return S3_SERVICE_NAME;
  }
}
