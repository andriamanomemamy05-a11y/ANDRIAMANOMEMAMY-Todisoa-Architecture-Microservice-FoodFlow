import { Injectable, OnModuleInit, OnModuleDestroy } from '@nestjs/common';
import { PrismaClient, Payment } from '@prisma/client';
import { CreatePaymentDto } from './dto/create-payment.dto';

@Injectable()
export class PaymentsService extends PrismaClient implements OnModuleInit, OnModuleDestroy {
  async onModuleInit() {
    await this.$connect();
  }

  async onModuleDestroy() {
    await this.$disconnect();
  }

  async create(dto: CreatePaymentDto): Promise<Payment> {
    const status = dto.amount > 1000 ? 'REFUSED' : 'AUTHORIZED';

    return this.payment.create({
      data: {
        orderId: dto.orderId,
        amount: dto.amount,
        status: status,
      },
    });
  }

  async findByOrderId(orderId: string): Promise<Payment> {
    const payment = await this.payment.findFirst({
      where: { orderId },
    });

    if (!payment) {
      return {
        id: 'none',
        orderId,
        amount: 0,
        status: 'REFUSED',
        createdAt: new Date(),
      };
    }

    return payment;
  }
}