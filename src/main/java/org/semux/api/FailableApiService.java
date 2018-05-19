/**
 * Copyright (c) 2017-2018 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.api;

import javax.ws.rs.core.Response;

public interface FailableApiService {

    Response failure(Response.Status status, String message);

}
