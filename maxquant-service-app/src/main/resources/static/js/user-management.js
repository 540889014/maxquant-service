document.addEventListener('DOMContentLoaded', function() {
    new Vue({
        el: '#app',
        data: {
            users: [],
            newUsername: '',
            newPassword: '',
            newRole: 'USER',
            isAdmin: false,
            showModal: false,
            selectedUser: null,
            updatePassword: ''
        },
        created() {
            this.fetchUsers();
            this.checkAdminStatus();
        },
        methods: {
            fetchUsers() {
                axios.get('/api/users/all')
                    .then(response => {
                        this.users = response.data;
                    })
                    .catch(error => {
                        console.error('获取用户列表失败:', error);
                        alert('获取用户列表失败');
                    });
            },
            checkAdminStatus() {
                // 这里应该从登录信息中获取当前用户的角色
                // 暂时假设从本地存储获取
                const currentUser = JSON.parse(localStorage.getItem('currentUser'));
                if (currentUser && currentUser.role === 'ADMIN') {
                    this.isAdmin = true;
                }
            },
            addUser() {
                if (!this.newUsername || !this.newPassword) {
                    alert('用户名和密码不能为空');
                    return;
                }
                axios.post('/api/users/add', null, {
                    params: {
                        username: this.newUsername,
                        password: this.newPassword,
                        role: this.newRole
                    }
                })
                .then(response => {
                    this.users.push(response.data);
                    this.newUsername = '';
                    this.newPassword = '';
                    this.newRole = 'USER';
                    alert('用户添加成功');
                })
                .catch(error => {
                    console.error('添加用户失败:', error);
                    alert('添加用户失败: ' + error.response.data);
                });
            },
            deleteUser(userId) {
                if (confirm('确认删除此用户吗？')) {
                    axios.delete(`/api/users/delete/${userId}`)
                        .then(response => {
                            this.users = this.users.filter(user => user.id !== userId);
                            alert('用户删除成功');
                        })
                        .catch(error => {
                            console.error('删除用户失败:', error);
                            alert('删除用户失败');
                        });
                }
            },
            openUpdatePasswordModal(user) {
                this.selectedUser = user;
                this.updatePassword = '';
                this.showModal = true;
            },
            closeModal() {
                this.showModal = false;
                this.selectedUser = null;
            },
            updatePassword() {
                if (!this.updatePassword) {
                    alert('新密码不能为空');
                    return;
                }
                axios.put(`/api/users/update-password/${this.selectedUser.id}`, null, {
                    params: {
                        newPassword: this.updatePassword
                    }
                })
                .then(response => {
                    this.closeModal();
                    alert('密码修改成功');
                })
                .catch(error => {
                    console.error('修改密码失败:', error);
                    alert('修改密码失败');
                });
            }
        }
    });
}); 