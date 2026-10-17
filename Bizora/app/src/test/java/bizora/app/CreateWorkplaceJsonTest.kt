package bizora.app

import bizora.app.data.remote.ApiClient
import bizora.app.data.remote.ApiResponse
import bizora.app.data.remote.CreateWorkplaceResponse
import bizora.app.data.remote.RemoteMembership
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Test

class CreateWorkplaceJsonTest {

    private val moshi = Moshi.Builder()
        .add(ApiClient.WorkplaceRoleAdapter())
        .add(ApiClient.WorkplaceMemberItemAdapter())
        .add(ApiClient.RemoteMembershipAdapter())
        .add(KotlinJsonAdapterFactory())
        .build()

    @Test
    fun parseCreateWorkplaceResponse_whenWorkplaceIdIsAnObject_succeedsWithoutError() {
        // Exact JSON structure from backend where workplaceId, userId, teamId are populated Mongoose/Prisma objects
        val json = """
            {
              "success": true,
              "data": {
                "workplace": {
                  "_id": "67090123456789abcdef0123",
                  "name": "Acme Workplace",
                  "workplaceCode": "BWP-1234-567",
                  "timezone": "Asia/Kolkata",
                  "status": "ACTIVE"
                },
                "member": {
                  "_id": "67090123456789abcdef0124",
                  "id": "67090123456789abcdef0124",
                  "workplaceId": {
                    "_id": "67090123456789abcdef0123",
                    "id": "67090123456789abcdef0123",
                    "name": "Acme Workplace",
                    "workplaceCode": "BWP-1234-567"
                  },
                  "userId": {
                    "_id": "67090123456789abcdef0125",
                    "id": "67090123456789abcdef0125",
                    "name": "Shivam"
                  },
                  "teamId": {
                    "_id": "67090123456789abcdef0126",
                    "name": "General"
                  },
                  "role": "EMPLOYER",
                  "name": "Shivam",
                  "status": "ACTIVE"
                },
                "team": {
                  "_id": "67090123456789abcdef0126",
                  "name": "General"
                }
              }
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, CreateWorkplaceResponse::class.java)
        val adapter = moshi.adapter<ApiResponse<CreateWorkplaceResponse>>(type)

        val response = adapter.fromJson(json)
        assertNotNull(response)
        assertTrue(response!!.success)
        assertNotNull(response.data)
        assertEquals("Acme Workplace", response.data!!.workplace.name)
        assertEquals("67090123456789abcdef0123", response.data!!.workplace.realId)
        assertNotNull(response.data!!.member)
        assertEquals("67090123456789abcdef0123", response.data!!.member!!.workplaceId)
        assertEquals("67090123456789abcdef0125", response.data!!.member!!.userId)
        assertEquals("EMPLOYER", response.data!!.member!!.role)
    }

    @Test
    fun parseCreateWorkplaceResponse_whenWorkplaceIdIsString_succeeds() {
        val json = """
            {
              "success": true,
              "data": {
                "workplace": {
                  "_id": "67090123456789abcdef0123",
                  "name": "Beta Workplace",
                  "workplaceCode": "BWP-9999-888"
                },
                "member": {
                  "_id": "67090123456789abcdef0124",
                  "workplaceId": "67090123456789abcdef0123",
                  "userId": "67090123456789abcdef0125",
                  "role": "EMPLOYER",
                  "name": "Shivam"
                }
              }
            }
        """.trimIndent()

        val type = Types.newParameterizedType(ApiResponse::class.java, CreateWorkplaceResponse::class.java)
        val adapter = moshi.adapter<ApiResponse<CreateWorkplaceResponse>>(type)

        val response = adapter.fromJson(json)
        assertNotNull(response)
        assertEquals("67090123456789abcdef0123", response!!.data!!.member!!.workplaceId)
    }

    @Test
    fun parseRemoteMembership_whenWorkplaceIdIsObject_succeeds() {
        val json = """
            {
              "id": "mem_123",
              "workplaceId": {
                "_id": "wp_456",
                "name": "HQ Branch",
                "workplaceCode": "BWP-1111-222"
              },
              "role": "EMPLOYER"
            }
        """.trimIndent()

        val adapter = moshi.adapter(RemoteMembership::class.java)
        val membership = adapter.fromJson(json)

        assertNotNull(membership)
        assertEquals("wp_456", membership!!.workplaceId)
        assertEquals("HQ Branch", membership.workplaceName)
        assertEquals("BWP-1111-222", membership.workplaceCode)
    }
}
