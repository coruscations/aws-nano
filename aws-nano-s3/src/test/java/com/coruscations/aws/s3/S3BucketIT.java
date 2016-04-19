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

import com.coruscations.aws.ConfigurationProvider;
import com.coruscations.aws.EmptyRestCommandResponse;
import com.coruscations.aws.s3.Grant.Permission;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.coruscations.aws.s3.Grant.Permission.FULL_CONTROL;
import static com.coruscations.aws.s3.Grant.Permission.READ;
import static com.coruscations.aws.s3.Grant.Permission.READ_ACP;
import static com.coruscations.aws.s3.Grant.Permission.WRITE;
import static com.coruscations.aws.s3.Grant.Permission.WRITE_ACP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(Enclosed.class)
public class S3BucketIT extends S3CommandIT {

  private static final Logger LOG = Logger.getLogger(S3BucketIT.class.getName());

  private static final ConfigurationProvider CONFIGURATION_PROVIDER =
      new ConfigurationProvider(createEnvironment());

  private static final S3ServiceCommands S3_SERVICE_COMMANDS =
      new S3ServiceCommands(CONFIGURATION_PROVIDER);
  private static final S3BucketCommands S3_BUCKET_COMMANDS =
      new S3BucketCommands(CONFIGURATION_PROVIDER);

  private static final SecureRandom RANDOM = new SecureRandom();

  public static class MakeBucket {

    @ClassRule
    public static final TestRule MINIO =
        new MinioRule(CONFIGURATION_PROVIDER, S3RestCommand.S3_SERVICE_NAME);

    private final String testBucketName = "test-" + new BigInteger(50, RANDOM).toString(32);

    @Test
    public void makeBucket() throws IOException {
      assertSuccessResponse(S3_BUCKET_COMMANDS.make(this.testBucketName, null));
      assertSuccessResponse(S3_SERVICE_COMMANDS.listBuckets());

      Collection<BucketsGet.Bucket> afterMakeBuckets =
          S3_SERVICE_COMMANDS.listBuckets().getBuckets();
      assertTrue("New bucket not created",
                 afterMakeBuckets.stream().anyMatch(b -> testBucketName.equals(b.getName())));

      removeBucket();
    }

    public void removeBucket() throws IOException {
      try {
        EmptyRestCommandResponse remove = S3_BUCKET_COMMANDS.remove(this.testBucketName);
        BucketsGet.Response afterRemove = S3_SERVICE_COMMANDS.listBuckets();
        Collection<BucketsGet.Bucket> afterRemoveBuckets = afterRemove.getBuckets();
        assertFalse("New bucket not removed",
                    afterRemoveBuckets.stream().anyMatch(b -> testBucketName.equals(b.getName())));
      } catch (Exception e) {
        LOG.log(Level.INFO, "Failed to remove created bucket", e);
      }
    }
  }

  public static class RemoveBucket extends WithBucket {

    @Test
    public void removeBucket() throws IOException {
      assertSuccessResponse(S3_BUCKET_COMMANDS.remove(TEST_BUCKET_NAME));

      BucketsGet.Response afterRemove = S3_SERVICE_COMMANDS.listBuckets();
      Collection<BucketsGet.Bucket> afterRemoveBuckets = afterRemove.getBuckets();
      assertFalse("New bucket not removed",
                  afterRemoveBuckets.stream().anyMatch(b -> TEST_BUCKET_NAME.equals(b.getName())));
    }
  }

  @RunWith(Theories.class)
  public static class BucketAcl extends WithBucket {

    //    private static final Grantee EMAIL_ADDRESS = new Grantee(Grantee.Type.EMAIL_ADDRESS,
//                                                             "mkw@upstreamdb.com", "Michael Werle");
    private static final Grantee ID =
        new Grantee(Grantee.Type.ID,
                    "114125fff24fb30eb9f6b38023aff1b74a7a494b0b784aa42424e964a36c5b45",
                    "accounts");
    private static final Grantee URI = new Grantee(Grantee.Type.URI,
                                                   "http://acs.amazonaws.com/groups/global/AuthenticatedUsers",
                                                   "Authenticated Users");

    @DataPoints
    public static Grantee[][] grantees() {
      return new Grantee[][]{
//          new Grantee[]{EMAIL_ADDRESS},
new Grantee[]{ID},
new Grantee[]{URI},

//          new Grantee[]{EMAIL_ADDRESS, ID},
//          new Grantee[]{EMAIL_ADDRESS, URI},
new Grantee[]{ID, URI}

//          new Grantee[]{EMAIL_ADDRESS, ID, URI}
      };
    }

    @DataPoints
    public static Permission[][] permissions() {
      return new Permission[][]{
          new Permission[]{READ},
          new Permission[]{READ_ACP},
          new Permission[]{WRITE},
          new Permission[]{WRITE_ACP},

          new Permission[]{READ, READ_ACP},
          new Permission[]{READ, WRITE},
          new Permission[]{READ, WRITE_ACP},
          new Permission[]{READ_ACP, WRITE},
          new Permission[]{READ_ACP, WRITE_ACP},
          new Permission[]{WRITE_ACP, WRITE},

          new Permission[]{READ, READ_ACP, WRITE},
          new Permission[]{READ, READ_ACP, WRITE_ACP},
          new Permission[]{READ, WRITE, WRITE_ACP},
          new Permission[]{READ_ACP, WRITE, WRITE_ACP},

          new Permission[]{READ, READ_ACP, WRITE, WRITE_ACP},

          new Permission[]{FULL_CONTROL}
      };
    }

    @BeforeClass
    public static void beforeClass() {
      Assume.assumeFalse("Minio does not support bucket ACLs", MINIO_RULE.isUseMinio());
    }

    @Test
    public void cannedAcl() throws IOException {
      for (BucketCannedAcl cannedAcl : BucketCannedAcl.values()) {
        Acl acl = new Acl(cannedAcl);
        assertSuccessResponse(S3_BUCKET_COMMANDS.setAcl(TEST_BUCKET_NAME, acl));
        BucketGetAcl.Response retrieved = S3_BUCKET_COMMANDS.getAcl(TEST_BUCKET_NAME);
        assertSuccessResponse(retrieved);
        // TODO: Figure out how to check the mapping between the canned ACLs and the permissions
      }
    }

    @Theory
    public void grants(Grantee[] grantees, Permission[] permissions) throws IOException {
      Set<Grant> grants = new LinkedHashSet<>();
      for (Grantee grantee : grantees) {
        for (Permission permission : permissions) {
          grants.add(new Grant(grantee, permission));
        }
      }
      assertSuccessResponse(S3_BUCKET_COMMANDS.setAcl(TEST_BUCKET_NAME, new Acl(grants)));
      BucketGetAcl.Response retrieved = S3_BUCKET_COMMANDS.getAcl(TEST_BUCKET_NAME);
      assertSuccessResponse(retrieved);
      List<Grant> retrievedGrants = retrieved.getAcl().getGrants();
      assertEquals("Unexpected number of grants", grants.size(), retrievedGrants.size());
      for (Grant retrievedGrant : retrievedGrants) {
        assertTrue("Unexpected grant", grants.contains(retrievedGrant));
      }
    }
  }

  public abstract static class WithBucket {

    protected static final WithBucketRule WITH_BUCKET_RULE =
        new WithBucketRule(CONFIGURATION_PROVIDER);
    protected static final MinioRule MINIO_RULE =
        new MinioRule(CONFIGURATION_PROVIDER, S3RestCommand.S3_SERVICE_NAME);

    @ClassRule
    public static final TestRule RULES = RuleChain.outerRule(MINIO_RULE).around(WITH_BUCKET_RULE);

    protected static final String TEST_BUCKET_NAME = WITH_BUCKET_RULE.getBucketName();

  }
}
